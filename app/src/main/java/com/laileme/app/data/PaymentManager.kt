package com.laileme.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class PaymentResult(
    val success: Boolean,
    val message: String,
    val orderId: String = "",
    val planType: String = "",
    val expiresAt: String? = null
)

data class VipStatus(
    val isVip: Boolean,
    val planType: String = "",
    val expiresAt: String? = null,
    val expired: Boolean = false
)

object PaymentManager {
    private const val BASE_URL = "http://47.123.5.171:8080"

    /** 创建付费订单 */
    suspend fun createOrder(planType: String, token: String): PaymentResult {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/api/payment/create")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val body = JSONObject().apply { put("planType", planType) }
                conn.outputStream.use { it.write(body.toString().toByteArray()) }

                val responseCode = conn.responseCode
                val response = if (responseCode == 200) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                }
                conn.disconnect()

                val json = JSONObject(response)
                if (json.optBoolean("success", false)) {
                    val data = json.optJSONObject("data")
                    PaymentResult(
                        success = true,
                        message = json.optString("message", "开通成功"),
                        orderId = data?.optString("orderId", "") ?: "",
                        planType = data?.optString("planType", "") ?: "",
                        expiresAt = if (data?.isNull("expiresAt") != false) null
                                    else data.optString("expiresAt")
                    )
                } else {
                    PaymentResult(false, json.optString("message", "支付失败"))
                }
            } catch (e: Exception) {
                PaymentResult(false, "网络错误：${e.localizedMessage}")
            }
        }
    }

    /** 查询VIP状态 */
    suspend fun checkVipStatus(token: String): VipStatus {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/api/payment/vip-status")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val responseCode = conn.responseCode
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                if (responseCode != 200) return@withContext VipStatus(false)

                val json = JSONObject(response)
                if (json.optBoolean("success", false)) {
                    val data = json.getJSONObject("data")
                    VipStatus(
                        isVip = data.optBoolean("isVip", false),
                        planType = data.optString("planType", ""),
                        expiresAt = if (data.isNull("expiresAt")) null
                                    else data.optString("expiresAt"),
                        expired = data.optBoolean("expired", false)
                    )
                } else {
                    VipStatus(false)
                }
            } catch (e: Exception) {
                VipStatus(false)
            }
        }
    }

    /** 保存本地VIP状态 */
    fun saveLocalVipStatus(context: Context, isVip: Boolean, planType: String = "") {
        context.getSharedPreferences("laileme_vip", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_vip", isVip)
            .putString("plan_type", planType)
            .apply()
    }

    /** 读取本地VIP状态 */
    fun isLocalVip(context: Context): Boolean {
        return context.getSharedPreferences("laileme_vip", Context.MODE_PRIVATE)
            .getBoolean("is_vip", false)
    }
}
