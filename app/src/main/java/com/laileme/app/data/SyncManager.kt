package com.laileme.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.laileme.app.data.entity.DiaryEntry
import com.laileme.app.data.entity.PeriodRecord
import com.laileme.app.data.entity.SleepRecord
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 数据同步管理器 - 所有登录用户自动同步数据到服务器
 * 特性：
 * - 登录后立即同步
 * - 数据变更后立即同步（防止卸载丢数据）
 * - 每5分钟定时同步兜底
 */
object SyncManager {
    private const val TAG = "SyncManager"
    private const val BASE_URL = "http://47.123.5.171:8080"
    private const val PREF_NAME = "laileme_sync"
    private const val KEY_LAST_SYNC = "last_sync_time"

    private var syncJob: Job? = null
    private var partnerPollJob: Job? = null
    private var token: String = ""
    private var appCtx: Context? = null
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** 设置认证token */
    fun setToken(t: String) {
        token = t
    }

    /** 获取上次同步时间 */
    fun getLastSyncTime(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_SYNC, null)
    }

    /** 启动自动同步（登录后调用）—— 不检查VIP，所有用户都同步 */
    fun startAutoSync(context: Context) {
        stopAutoSync()
        if (token.isBlank()) return
        appCtx = context.applicationContext

        syncJob = syncScope.launch {
            // 立即同步一次
            try {
                performSync(context)
                Log.i(TAG, "初始同步成功")
            } catch (e: Exception) {
                Log.e(TAG, "初始同步失败", e)
            }

            // 每5分钟同步一次兜底
            while (isActive) {
                delay(5 * 60 * 1000L)
                try {
                    performSync(context)
                } catch (e: Exception) {
                    Log.e(TAG, "定时同步失败", e)
                }
            }
        }
    }

    /** 停止自动同步 */
    fun stopAutoSync() {
        syncJob?.cancel()
        syncJob = null
        stopPartnerPoll()
    }

    /** 启动伴侣数据变更轮询（男性用户自动调用） */
    fun startPartnerPoll(context: Context) {
        stopPartnerPoll()
        if (token.isBlank()) return
        val ctx = context.applicationContext

        partnerPollJob = syncScope.launch {
            // 延迟5秒再开始，等登录流程稳定
            delay(5000L)
            while (isActive) {
                try {
                    val user = AuthManager.userState.value
                    if (user != null && user.gender == "male") {
                        val (hasUpdate, fromNickname) = PartnerManager.checkPartnerUpdate(token)
                        if (hasUpdate) {
                            Log.i(TAG, "伴侣数据有更新，来自: $fromNickname，开始同步...")
                            // 发送通知
                            com.laileme.app.notification.NotificationHelper.sendPartnerUpdate(ctx, fromNickname)
                            // 触发ViewModel重新拉取伴侣数据（通过广播）
                            val intent = android.content.Intent("com.laileme.app.PARTNER_DATA_UPDATED")
                            intent.setPackage(ctx.packageName)
                            ctx.sendBroadcast(intent)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "伴侣轮询失败", e)
                }
                delay(60 * 1000L) // 每60秒检查一次
            }
        }
    }

    /** 停止伴侣轮询 */
    fun stopPartnerPoll() {
        partnerPollJob?.cancel()
        partnerPollJob = null
    }

    /** 数据变更时立即触发同步（在数据保存后调用） */
    fun triggerImmediateSync() {
        val ctx = appCtx ?: return
        if (token.isBlank()) return
        syncScope.launch {
            try {
                performSync(ctx)
                Log.i(TAG, "数据变更立即同步成功")
            } catch (e: Exception) {
                Log.e(TAG, "数据变更立即同步失败", e)
            }
        }
    }

    /** 手动触发同步 */
    suspend fun manualSync(context: Context): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                performSync(context)
                Result.success("同步成功")
            } catch (e: Exception) {
                Log.e(TAG, "手动同步失败", e)
                Result.failure(e)
            }
        }
    }

    /** 发送心跳（报活，用于数据保留策略判断） */
    private suspend fun sendHeartbeat() {
        if (token.isBlank()) return
        try {
            val url = URL("$BASE_URL/api/heartbeat")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.outputStream.use { it.write("{}".toByteArray()) }
            val code = conn.responseCode
            conn.disconnect()
            if (code == 200) {
                Log.d(TAG, "心跳发送成功")
            }
        } catch (e: Exception) {
            Log.w(TAG, "心跳发送失败: ${e.message}")
        }
    }

    /** 执行同步：上传本地数据到服务器 */
    private suspend fun performSync(context: Context) {
        if (token.isBlank()) throw Exception("未登录")
        // 同步时顺便发送心跳
        sendHeartbeat()

        val db = AppDatabase.getDatabase(context)

            // 收集本地所有数据
            val periods = db.periodDao().getAllList()
            val diaries = db.diaryDao().getAllList()
            val sleepRecords = db.sleepDao().getAll().first()
            val secretRecords = db.secretDao().getAll().first()

            // 收集个人档案数据
        val profilePrefs = context.getSharedPreferences("laileme_profile", Context.MODE_PRIVATE)
        val profileData = JSONObject().apply {
            put("nickname", profilePrefs.getString("nickname", "") ?: "")
            put("birth_year", profilePrefs.getString("birth_year", "") ?: "")
            put("birth_month", profilePrefs.getString("birth_month", "") ?: "")
            put("birth_day", profilePrefs.getString("birth_day", "") ?: "")
            put("height", profilePrefs.getString("height", "") ?: "")
            put("weight", profilePrefs.getString("weight", "") ?: "")
            put("blood_type", profilePrefs.getString("blood_type", "") ?: "")
        }

        // 收集经期设置数据
        val settingsPrefs = context.getSharedPreferences("laileme_settings", Context.MODE_PRIVATE)
        val settingsData = JSONObject().apply {
            put("has_setup", settingsPrefs.getBoolean("has_setup", false))
            put("tracking_mode", settingsPrefs.getString("tracking_mode", "auto") ?: "auto")
            put("cycle_length", settingsPrefs.getInt("cycle_length", 28))
            put("period_length", settingsPrefs.getInt("period_length", 5))
        }

        // 收集药物提醒数据
        val discoverPrefs = context.getSharedPreferences("laileme_discover", Context.MODE_PRIVATE)
        val discoverData = JSONObject().apply {
            put("medications", discoverPrefs.getString("medications", "") ?: "")
            put("water_goal", discoverPrefs.getInt("water_goal", 8))
            put("habits", discoverPrefs.getString("habits", "") ?: "")
            put("habit_checkins", discoverPrefs.getString("habit_checkins", "") ?: "")
        }

        // 收集步数/运动数据（最近7天）
        val userId = AuthManager.userState.value?.userId ?: "default"
        val healthPrefs = context.getSharedPreferences("laileme_health_$userId", Context.MODE_PRIVATE)
        val healthData = JSONObject()
        val cal = java.util.Calendar.getInstance()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        for (i in 0 until 7) {
            val dayKey = sdf.format(cal.time)
            val steps = healthPrefs.getInt("steps_$dayKey", 0)
            val calories = healthPrefs.getInt("calories_$dayKey", 0)
            val exercise = healthPrefs.getInt("exercise_$dayKey", 0)
            if (steps > 0 || calories > 0) {
                healthData.put(dayKey, JSONObject().apply {
                    put("steps", steps)
                    put("calories", calories)
                    put("exercise", exercise)
                })
            }
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }

        // 安全检查：如果本地数据和设置全空，可能是刚装好还没恢复，跳过上传
        val hasProfileData = profilePrefs.getString("nickname", "")?.isNotEmpty() == true ||
                profilePrefs.getString("birth_year", "")?.isNotEmpty() == true
        val hasSettings = settingsPrefs.getBoolean("has_setup", false)
        val hasMedications = discoverPrefs.getString("medications", "")?.isNotEmpty() == true

        if (periods.isEmpty() && diaries.isEmpty() && sleepRecords.isEmpty() &&
                !hasProfileData && !hasSettings && !hasMedications) {
            Log.w(TAG, "本地所有数据为空，跳过上传避免覆盖服务器数据")
            return
        }

        // 构建JSON
        val json = JSONObject().apply {
            put("profile", profileData)
            put("settings", settingsData)
            put("discover", discoverData)
            put("healthData", healthData)
            put("periodRecords", JSONArray().apply {
                periods.forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("startDate", r.startDate)
                        put("endDate", r.endDate ?: JSONObject.NULL)
                        put("cycleLength", r.cycleLength)
                        put("periodLength", r.periodLength)
                        put("symptoms", r.symptoms)
                        put("mood", r.mood)
                        put("notes", r.notes)
                    })
                }
            })
            put("diaryEntries", JSONArray().apply {
                diaries.forEach { d ->
                    put(JSONObject().apply {
                        put("id", d.id)
                        put("date", d.date)
                        put("mood", d.mood)
                        put("symptoms", d.symptoms)
                        put("notes", d.notes)
                        put("flowLevel", d.flowLevel)
                        put("flowColor", d.flowColor)
                        put("painLevel", d.painLevel)
                        put("breastPain", d.breastPain)
                        put("digestive", d.digestive)
                        put("backPain", d.backPain)
                        put("headache", d.headache)
                        put("fatigue", d.fatigue)
                        put("skinCondition", d.skinCondition)
                        put("temperature", d.temperature)
                        put("appetite", d.appetite)
                        put("discharge", d.discharge)
                    })
                }
            })
            put("sleepRecords", JSONArray().apply {
                sleepRecords.forEach { s ->
                    put(JSONObject().apply {
                        put("date", s.date)
                        put("bedtime", s.bedtime)
                        put("waketime", s.waketime)
                    })
                }
            })
            put("secretRecords", JSONArray().apply {
                secretRecords.forEach { r ->
                    put(JSONObject().apply {
                        put("date", r.date)
                        put("hadSex", r.hadSex)
                        put("protection", r.protection)
                        put("feeling", r.feeling)
                        put("mood", r.mood)
                        put("notes", r.notes)
                    })
                }
            })
        }

        Log.i(TAG, "准备上传: periods=${periods.size}, diaries=${diaries.size}, sleep=${sleepRecords.size}, secrets=${secretRecords.size}")

        // 发送到服务器
        val url = URL("$BASE_URL/api/sync/upload")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        conn.outputStream.use { it.write(json.toString().toByteArray()) }

        val responseCode = conn.responseCode
        val response = if (responseCode == 200) {
            conn.inputStream.bufferedReader().readText()
        } else {
            conn.errorStream?.bufferedReader()?.readText() ?: "error"
        }
        conn.disconnect()

        if (responseCode == 200) {
            val resp = JSONObject(response)
            if (resp.optBoolean("success", false)) {
                val lastSync = resp.optJSONObject("data")?.optString("lastSync", "") ?: ""
                getPrefs(context).edit().putString(KEY_LAST_SYNC, lastSync).apply()
                Log.i(TAG, "上传成功: $lastSync (periods=${periods.size}, diaries=${diaries.size}, sleep=${sleepRecords.size}, secrets=${secretRecords.size})")
            } else {
                throw Exception(resp.optString("message", "同步失败"))
            }
        } else {
            throw Exception("服务器错误: $responseCode - $response")
        }
    }

    /** 从服务器下载数据到本地（恢复数据） */
    suspend fun downloadAndRestore(context: Context): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (token.isBlank()) throw Exception("未登录")

                Log.i(TAG, "开始从服务器下载恢复数据...")

                val url = URL("$BASE_URL/api/sync/download")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val responseCode = conn.responseCode
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                if (responseCode != 200) throw Exception("服务器错误: $responseCode")

                val resp = JSONObject(response)
                if (!resp.optBoolean("success", false)) throw Exception(resp.optString("message"))

                val data = resp.getJSONObject("data")
                val db = AppDatabase.getDatabase(context)

                // 恢复个人档案
                val profile = data.optJSONObject("profile")
                if (profile != null) {
                    val profilePrefs = context.getSharedPreferences("laileme_profile", Context.MODE_PRIVATE)
                    profilePrefs.edit()
                        .putString("nickname", profile.optString("nickname", ""))
                        .putString("birth_year", profile.optString("birth_year", ""))
                        .putString("birth_month", profile.optString("birth_month", ""))
                        .putString("birth_day", profile.optString("birth_day", ""))
                        .putString("height", profile.optString("height", ""))
                        .putString("weight", profile.optString("weight", ""))
                        .putString("blood_type", profile.optString("blood_type", ""))
                        .apply()
                    Log.i(TAG, "个人档案恢复成功")
                }

                // 恢复经期设置
                val settings = data.optJSONObject("settings")
                if (settings != null) {
                    val settingsPrefs = context.getSharedPreferences("laileme_settings", Context.MODE_PRIVATE)
                    settingsPrefs.edit()
                        .putBoolean("has_setup", settings.optBoolean("has_setup", false))
                        .putString("tracking_mode", settings.optString("tracking_mode", "auto"))
                        .putInt("cycle_length", settings.optInt("cycle_length", 28))
                        .putInt("period_length", settings.optInt("period_length", 5))
                        .apply()
                    Log.i(TAG, "经期设置恢复成功")
                }

                // 恢复药物提醒和发现页数据
                val discover = data.optJSONObject("discover")
                if (discover != null) {
                    val discoverPrefs = context.getSharedPreferences("laileme_discover", Context.MODE_PRIVATE)
                    discoverPrefs.edit()
                        .putString("medications", discover.optString("medications", ""))
                        .putInt("water_goal", discover.optInt("water_goal", 8))
                        .putString("habits", discover.optString("habits", ""))
                        .putString("habit_checkins", discover.optString("habit_checkins", ""))
                        .apply()
                    Log.i(TAG, "药物提醒和习惯打卡恢复成功")
                }

            // 恢复经期记录（先清空再插入，避免重复）
            val periodsArr = data.optJSONArray("periodRecords")
            if (periodsArr != null && periodsArr.length() > 0) {
                db.periodDao().deleteAllRecords()
                for (i in 0 until periodsArr.length()) {
                    val p = periodsArr.getJSONObject(i)
                    db.periodDao().insert(PeriodRecord(
                        id = 0,
                        startDate = p.getLong("startDate"),
                        endDate = if (p.isNull("endDate")) null else p.getLong("endDate"),
                        cycleLength = p.optInt("cycleLength", 28),
                        periodLength = p.optInt("periodLength", 5),
                        symptoms = p.optString("symptoms", ""),
                        mood = p.optString("mood", ""),
                        notes = p.optString("notes", "")
                    ))
                }
                Log.i(TAG, "恢复经期记录: ${periodsArr.length()} 条")
            }

                // 恢复日记（先清空再插入，避免重复）
                val diariesArr = data.optJSONArray("diaryEntries")
                if (diariesArr != null && diariesArr.length() > 0) {
                db.diaryDao().deleteAll()
                for (i in 0 until diariesArr.length()) {
                        val d = diariesArr.getJSONObject(i)
                        db.diaryDao().insert(DiaryEntry(
                            id = 0,
                            date = d.getLong("date"),
                            mood = d.optString("mood", ""),
                            symptoms = d.optString("symptoms", ""),
                            notes = d.optString("notes", ""),
                            flowLevel = d.optInt("flowLevel", 0),
                            flowColor = d.optString("flowColor", ""),
                            painLevel = d.optInt("painLevel", 0),
                            breastPain = d.optInt("breastPain", 0),
                            digestive = d.optInt("digestive", 0),
                            backPain = d.optInt("backPain", 0),
                            headache = d.optInt("headache", 0),
                            fatigue = d.optInt("fatigue", 0),
                            skinCondition = d.optString("skinCondition", ""),
                            temperature = d.optString("temperature", ""),
                            appetite = d.optInt("appetite", 0),
                            discharge = d.optString("discharge", "")
                        ))
                    }
                    Log.i(TAG, "恢复日记: ${diariesArr.length()} 条")
                }

                // 恢复睡眠记录（先清空再插入，避免重复）
                val sleepArr = data.optJSONArray("sleepRecords")
                if (sleepArr != null && sleepArr.length() > 0) {
                db.sleepDao().deleteAll()
                for (i in 0 until sleepArr.length()) {
                        val s = sleepArr.getJSONObject(i)
                        db.sleepDao().insert(SleepRecord(
                            date = s.getString("date"),
                            bedtime = s.optString("bedtime", ""),
                            waketime = s.optString("waketime", "")
                        ))
                    }
                    Log.i(TAG, "恢复睡眠记录: ${sleepArr.length()} 条")
                }

                // 恢复私密日记记录
                val secretsArr = data.optJSONArray("secretRecords")
                if (secretsArr != null && secretsArr.length() > 0) {
                    db.secretDao().deleteAll()
                    for (i in 0 until secretsArr.length()) {
                        val s = secretsArr.getJSONObject(i)
                        db.secretDao().insert(com.laileme.app.data.entity.SecretRecord(
                            date = s.getLong("date"),
                            hadSex = s.optBoolean("hadSex", false),
                            protection = s.optString("protection", ""),
                            feeling = s.optInt("feeling", 0),
                            mood = s.optString("mood", ""),
                            notes = s.optString("notes", "")
                        ))
                    }
                    Log.i(TAG, "恢复私密记录: ${secretsArr.length()} 条")
                }

                // 恢复步数/运动数据
                val healthObj = data.optJSONObject("healthData")
                if (healthObj != null && healthObj.length() > 0) {
            val restoreUserId = AuthManager.userState.value?.userId ?: "default"
            val healthPrefs = context.getSharedPreferences("laileme_health_$restoreUserId", Context.MODE_PRIVATE)
                    val editor = healthPrefs.edit()
                    val keys = healthObj.keys()
                    var healthCount = 0
                    while (keys.hasNext()) {
                        val dayKey = keys.next()
                        val dayData = healthObj.optJSONObject(dayKey) ?: continue
                        val steps = dayData.optInt("steps", 0)
                        val calories = dayData.optInt("calories", 0)
                        val exercise = dayData.optInt("exercise", 0)
                        // 只恢复比本地大的值（避免覆盖今天已有的更新数据）
                        val localSteps = healthPrefs.getInt("steps_$dayKey", 0)
                        if (steps > localSteps) {
                            editor.putInt("steps_$dayKey", steps)
                            editor.putInt("calories_$dayKey", calories)
                            editor.putInt("exercise_$dayKey", exercise)
                            healthCount++
                        }
                    }
                    editor.apply()
                    Log.i(TAG, "恢复步数数据: $healthCount 天")
                }

                val totalRecords = (periodsArr?.length() ?: 0) + (diariesArr?.length() ?: 0) + (sleepArr?.length() ?: 0) + (secretsArr?.length() ?: 0)
                Log.i(TAG, "数据恢复完成，共 $totalRecords 条记录")
                Result.success("数据恢复成功，共 $totalRecords 条记录")
            } catch (e: Exception) {
                Log.e(TAG, "下载恢复失败", e)
                Result.failure(e)
            }
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}
