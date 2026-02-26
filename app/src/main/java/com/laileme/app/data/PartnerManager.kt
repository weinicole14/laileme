package com.laileme.app.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * 伴侣信息数据类
 */
data class PartnerInfo(
    val bound: Boolean = false,
    val partnerId: String = "",
    val partnerUid: String = "",
    val partnerNickname: String = "",
    val partnerAvatarUrl: String = "",
    val partnerGender: String = "female",
    val partnerMode: Boolean = false,
    val boundAt: Long = 0L  // 绑定时间戳（毫秒）
)

/**
 * 绑定请求数据类
 */
data class PartnerRequest(
    val fromId: Int = 0,
    val fromUid: String = "",
    val fromNickname: String = "",
    val fromAvatarUrl: String = "",
    val fromGender: String = "male",
    val timestamp: Long = 0
)

/**
 * 伴侣经期数据
 */
data class PartnerData(
    val partnerNickname: String = "",
    val periodRecords: List<JSONObject> = emptyList(),
    val sleepRecords: List<JSONObject> = emptyList(),
    val lastSync: String? = null
)

/**
 * 伴侣管理器 - 绑定请求/确认/拒绝/解绑
 */
object PartnerManager {
    private const val TAG = "PartnerManager"
    private const val BASE_URL = "http://47.123.5.171:8080"

    /** 通用POST请求 */
    private suspend fun postRequest(endpoint: String, token: String, body: JSONObject = JSONObject()): JSONObject {
        return withContext(Dispatchers.IO) {
            val url = URL("$BASE_URL$endpoint")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val responseCode = conn.responseCode
            val response = if (responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: "{}"
            }
            conn.disconnect()
            JSONObject(response)
        }
    }

    /** 通用GET请求 */
    private suspend fun getRequest(endpoint: String, token: String): JSONObject {
        return withContext(Dispatchers.IO) {
            val url = URL("$BASE_URL$endpoint")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            val responseCode = conn.responseCode
            val response = if (responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: "{}"
            }
            conn.disconnect()
            JSONObject(response)
        }
    }

    /** 开关伴侣模式 */
    suspend fun setPartnerMode(token: String, enabled: Boolean): Pair<Boolean, String> {
        return try {
            val body = JSONObject().apply { put("enabled", enabled) }
            val json = postRequest("/api/partner/mode", token, body)
            if (json.optInt("code") == 200) {
                Pair(true, json.optString("message", "操作成功"))
            } else {
                Pair(false, json.optString("message", "操作失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "mode err", e)
            Pair(false, "网络错误：${e.localizedMessage}")
        }
    }

    /** 发送绑定请求（不直接绑定） */
    suspend fun sendBindRequest(token: String, partnerUid: String): Pair<Boolean, String> {
        return try {
            val body = JSONObject().apply { put("partnerUid", partnerUid) }
            val json = postRequest("/api/partner/request", token, body)
            if (json.optInt("code") == 200) {
                Pair(true, json.optString("message", "请求已发送"))
            } else {
                Pair(false, json.optString("message", "发送失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "request err", e)
            Pair(false, "网络错误：${e.localizedMessage}")
        }
    }

    /** 获取待处理的绑定请求 */
    suspend fun getPendingRequests(token: String): Pair<Boolean, List<PartnerRequest>> {
        return try {
            val json = getRequest("/api/partner/pending", token)
            if (json.optInt("code") == 200) {
                val data = json.optJSONObject("data") ?: return Pair(true, emptyList())
                val arr = data.optJSONArray("requests") ?: JSONArray()
                val list = mutableListOf<PartnerRequest>()
                for (i in 0 until arr.length()) {
                    val r = arr.getJSONObject(i)
                    list.add(PartnerRequest(
                        fromId = r.optInt("fromId"),
                        fromUid = r.optString("fromUid", ""),
                        fromNickname = r.optString("fromNickname", ""),
                        fromAvatarUrl = r.optString("fromAvatarUrl", ""),
                        fromGender = r.optString("fromGender", "male"),
                        timestamp = r.optLong("timestamp", 0)
                    ))
                }
                Pair(true, list)
            } else {
                Pair(false, emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "pending err", e)
            Pair(false, emptyList())
        }
    }

    /** 同意绑定请求 */
    suspend fun acceptRequest(token: String, fromId: Int): Pair<Boolean, String> {
        return try {
            val body = JSONObject().apply { put("fromId", fromId) }
            val json = postRequest("/api/partner/accept", token, body)
            if (json.optInt("code") == 200) {
                Pair(true, json.optString("message", "绑定成功"))
            } else {
                Pair(false, json.optString("message", "操作失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "accept err", e)
            Pair(false, "网络错误：${e.localizedMessage}")
        }
    }

    /** 拒绝绑定请求 */
    suspend fun rejectRequest(token: String, fromId: Int): Pair<Boolean, String> {
        return try {
            val body = JSONObject().apply { put("fromId", fromId) }
            val json = postRequest("/api/partner/reject", token, body)
            if (json.optInt("code") == 200) {
                Pair(true, "已拒绝")
            } else {
                Pair(false, json.optString("message", "操作失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "reject err", e)
            Pair(false, "网络错误：${e.localizedMessage}")
        }
    }

    /** 解绑伴侣 */
    suspend fun unbindPartner(token: String): Pair<Boolean, String> {
        return try {
            val json = postRequest("/api/partner/unbind", token)
            if (json.optInt("code") == 200) {
                Pair(true, json.optString("message", "已解绑"))
            } else {
                Pair(false, json.optString("message", "解绑失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "unbind err", e)
            Pair(false, "网络错误：${e.localizedMessage}")
        }
    }

    /** 获取伴侣信息 */
    suspend fun getPartnerInfo(token: String): PartnerInfo {
        return try {
            val json = getRequest("/api/partner/info", token)
            if (json.optInt("code") != 200) return PartnerInfo()
            val data = json.optJSONObject("data") ?: return PartnerInfo()
            PartnerInfo(
                bound = data.optBoolean("bound", false),
                partnerId = data.optString("partnerId", ""),
                partnerUid = data.optString("partnerUid", ""),
                partnerNickname = data.optString("partnerNickname", ""),
                partnerAvatarUrl = data.optString("partnerAvatarUrl", ""),
                partnerGender = data.optString("partnerGender", "female"),
                partnerMode = data.optBoolean("partnerMode", false),
                boundAt = data.optLong("boundAt", 0L)
            )
        } catch (e: Exception) {
            Log.e(TAG, "info err", e)
            PartnerInfo()
        }
    }

    /** 检查伴侣数据是否有更新（男方轮询） */
    suspend fun checkPartnerUpdate(token: String): Pair<Boolean, String> {
        return try {
            val json = getRequest("/api/partner/check-update", token)
            if (json.optInt("code") == 200) {
                val data = json.optJSONObject("data") ?: return Pair(false, "")
                val hasUpdate = data.optBoolean("hasUpdate", false)
                val fromNickname = data.optString("fromNickname", "")
                Pair(hasUpdate, fromNickname)
            } else {
                Pair(false, "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "check-update err", e)
            Pair(false, "")
        }
    }

    /** 获取伴侣的经期数据 */
    suspend fun getPartnerData(token: String): PartnerData? {
        return try {
            val json = getRequest("/api/partner/data", token)
            if (json.optInt("code") != 200) return null
            val data = json.optJSONObject("data") ?: return null
            val periods = mutableListOf<JSONObject>()
            val periodsArr = data.optJSONArray("periodRecords")
            if (periodsArr != null) {
                for (i in 0 until periodsArr.length()) {
                    periods.add(periodsArr.getJSONObject(i))
                }
            }
            val sleeps = mutableListOf<JSONObject>()
            val sleepArr = data.optJSONArray("sleepRecords")
            if (sleepArr != null) {
                for (i in 0 until sleepArr.length()) {
                    sleeps.add(sleepArr.getJSONObject(i))
                }
            }
            PartnerData(
                partnerNickname = data.optString("partnerNickname", ""),
                periodRecords = periods,
                sleepRecords = sleeps,
                lastSync = data.optString("lastSync", null)
            )
        } catch (e: Exception) {
            Log.e(TAG, "data err", e)
            null
        }
    }

    /** 发送关怀消息给伴侣 */
    suspend fun sendCareMessage(token: String, message: String): Pair<Boolean, String> {
        return try {
            val body = JSONObject().apply { put("message", message) }
            val json = postRequest("/api/partner/care", token, body)
            if (json.optInt("code") == 200) {
                Pair(true, json.optString("message", "发送成功"))
            } else {
                Pair(false, json.optString("message", "发送失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "care err", e)
            Pair(false, "网络错误：${e.localizedMessage}")
        }
    }
}
