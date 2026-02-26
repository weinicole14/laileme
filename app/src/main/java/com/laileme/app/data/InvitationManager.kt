package com.laileme.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** 我的邀请信息 */
data class InvitationInfo(
    val myCode: String = "",
    val invitedCount: Int = 0,
    val rewardDays: Int = 0,
    val usedCode: String? = null
)

/** 使用邀请码结果 */
data class InvitationResult(
    val success: Boolean,
    val message: String,
    val rewardDays: Int = 0
)

/** 创建特殊邀请码结果 */
data class SpecialCodeResult(
    val success: Boolean,
    val message: String,
    val code: String = ""
)

/**
 * 邀请码管理器 —— 对接服务器邀请码接口
 *
 * 功能：
 * 1. 获取用户自己的邀请码
 * 2. 填写他人邀请码 → 双方获得VIP奖励
 * 3. 开发者创建特殊邀请码（需管理员密码）
 */
object InvitationManager {
    private const val BASE_URL = "http://47.123.5.171:8080"

    /** 获取我的邀请码和邀请统计 */
    suspend fun getMyInvitation(token: String): InvitationInfo {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/api/invite/my-code")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val responseCode = conn.responseCode
                val response = if (responseCode == 200) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                }
                conn.disconnect()

                if (responseCode != 200) return@withContext InvitationInfo()

                val json = JSONObject(response)
                if (json.optBoolean("success", false)) {
                    val data = json.getJSONObject("data")
                    InvitationInfo(
                        myCode = data.optString("code", ""),
                        invitedCount = data.optInt("invitedCount", 0),
                        rewardDays = data.optInt("totalRewardDays", 0),
                        usedCode = if (data.isNull("usedCode")) null
                                   else data.optString("usedCode")
                    )
                } else {
                    InvitationInfo()
                }
            } catch (e: Exception) {
                android.util.Log.e("InvitationManager", "getMyInvitation error", e)
                InvitationInfo()
            }
        }
    }

    /** 使用邀请码 → 邀请人和被邀请人都获得VIP奖励 */
    suspend fun useInvitationCode(token: String, code: String): InvitationResult {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/api/invite/redeem")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val body = JSONObject().apply { put("code", code) }
                conn.outputStream.use { it.write(body.toString().toByteArray()) }

                val responseCode = conn.responseCode
                val response = if (responseCode == 200) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                }
                conn.disconnect()

                val json = JSONObject(response)
                InvitationResult(
                    success = json.optBoolean("success", false),
                    message = json.optString("message",
                        if (json.optBoolean("success", false)) "兑换成功" else "兑换失败"),
                    rewardDays = json.optJSONObject("data")?.optInt("rewardDays", 0) ?: 0
                )
            } catch (e: Exception) {
                InvitationResult(false, "网络错误：${e.localizedMessage}")
            }
        }
    }

    /** 开发者创建特殊邀请码（需要管理员密钥验证） */
    suspend fun createSpecialCode(
        adminKey: String,
        code: String,
        rewardDays: Int = 30,
        maxUses: Int = 0,
        description: String = ""
    ): SpecialCodeResult {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/api/invite/create-special")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val body = JSONObject().apply {
                    put("adminKey", adminKey)
                    put("code", code)
                    put("rewardDays", rewardDays)
                    put("maxUses", maxUses)
                    put("description", description)
                }
                conn.outputStream.use { it.write(body.toString().toByteArray()) }

                val responseCode = conn.responseCode
                val response = if (responseCode == 200) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                }
                conn.disconnect()

                val json = JSONObject(response)
                SpecialCodeResult(
                    success = json.optBoolean("success", false),
                    message = json.optString("message", ""),
                    code = json.optJSONObject("data")?.optString("code", "") ?: ""
                )
            } catch (e: Exception) {
                SpecialCodeResult(false, "网络错误：${e.localizedMessage}")
            }
        }
    }
}
