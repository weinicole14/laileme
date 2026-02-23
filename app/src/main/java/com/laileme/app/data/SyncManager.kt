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
 * 数据同步管理器 - 付费用户自动同步数据到服务器
 */
object SyncManager {
    private const val TAG = "SyncManager"
    private const val BASE_URL = "http://47.123.5.171:8080"
    private const val PREF_NAME = "laileme_sync"
    private const val KEY_LAST_SYNC = "last_sync_time"

    private var syncJob: Job? = null
    private var token: String = ""

    /** 设置认证token */
    fun setToken(t: String) {
        token = t
    }

    /** 获取上次同步时间 */
    fun getLastSyncTime(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_SYNC, null)
    }

    /** 启动自动同步（登录后调用） */
    fun startAutoSync(context: Context) {
        stopAutoSync()
        if (token.isBlank()) return

        // 检查是否付费用户
        val isPaid = context.getSharedPreferences("laileme_vip", Context.MODE_PRIVATE)
            .getBoolean("is_vip", false)
        if (!isPaid) return

        syncJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            // 立即同步一次
            try { performSync(context) } catch (e: Exception) { Log.e(TAG, "sync err", e) }

            // 然后每15分钟同步一次
            while (isActive) {
                delay(15 * 60 * 1000L)
                try { performSync(context) } catch (e: Exception) { Log.e(TAG, "auto sync err", e) }
            }
        }
    }

    /** 停止自动同步 */
    fun stopAutoSync() {
        syncJob?.cancel()
        syncJob = null
    }

    /** 手动触发同步 */
    suspend fun manualSync(context: Context): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                performSync(context)
                Result.success("同步成功")
            } catch (e: Exception) {
                Log.e(TAG, "manual sync err", e)
                Result.failure(e)
            }
        }
    }

    /** 执行同步：上传本地数据到服务器 */
    private suspend fun performSync(context: Context) {
        if (token.isBlank()) throw Exception("未登录")

        val db = AppDatabase.getDatabase(context)

        // 收集本地所有数据
        val periods = db.periodDao().getAllList()
        val diaries = db.diaryDao().getAllList()
        val sleepRecords = db.sleepDao().getAll().first()

        // 构建JSON
        val json = JSONObject().apply {
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
        }

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
                Log.i(TAG, "sync upload ok: $lastSync")
            } else {
                throw Exception(resp.optString("message", "同步失败"))
            }
        } else {
            throw Exception("服务器错误: $responseCode")
        }
    }

    /** 从服务器下载数据到本地（恢复数据） */
    suspend fun downloadAndRestore(context: Context): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (token.isBlank()) throw Exception("未登录")

                val url = URL("$BASE_URL/api/sync/download")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val responseCode = conn.responseCode
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                if (responseCode != 200) throw Exception("服务器错误")

                val resp = JSONObject(response)
                if (!resp.optBoolean("success", false)) throw Exception(resp.optString("message"))

                val data = resp.getJSONObject("data")
                val db = AppDatabase.getDatabase(context)

                // 恢复经期记录
                val periodsArr = data.optJSONArray("periodRecords")
                if (periodsArr != null && periodsArr.length() > 0) {
                    for (i in 0 until periodsArr.length()) {
                        val p = periodsArr.getJSONObject(i)
                        db.periodDao().insert(PeriodRecord(
                            id = 0, // 自动生成新ID
                            startDate = p.getLong("startDate"),
                            endDate = if (p.isNull("endDate")) null else p.getLong("endDate"),
                            cycleLength = p.optInt("cycleLength", 28),
                            periodLength = p.optInt("periodLength", 5),
                            symptoms = p.optString("symptoms", ""),
                            mood = p.optString("mood", ""),
                            notes = p.optString("notes", "")
                        ))
                    }
                }

                // 恢复日记
                val diariesArr = data.optJSONArray("diaryEntries")
                if (diariesArr != null && diariesArr.length() > 0) {
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
                }

                // 恢复睡眠记录
                val sleepArr = data.optJSONArray("sleepRecords")
                if (sleepArr != null && sleepArr.length() > 0) {
                    for (i in 0 until sleepArr.length()) {
                        val s = sleepArr.getJSONObject(i)
                        db.sleepDao().insert(SleepRecord(
                            date = s.getString("date"),
                            bedtime = s.optString("bedtime", ""),
                            waketime = s.optString("waketime", "")
                        ))
                    }
                }

                Result.success("数据恢复成功")
            } catch (e: Exception) {
                Log.e(TAG, "download err", e)
                Result.failure(e)
            }
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}
