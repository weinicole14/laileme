package com.laileme.app.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// ── Retrofit API 定义 ──
interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse

    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") token: String): ApiResponse

    @GET("api/user/profile")
    suspend fun getProfile(@Header("Authorization") token: String): ApiResponse

    @PUT("api/user/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body body: UpdateProfileRequest
    ): ApiResponse
}

// ── 请求数据类 ──
data class LoginRequest(val username: String, val password: String)
data class RegisterRequest(val username: String, val password: String, val nickname: String)
data class UpdateProfileRequest(val nickname: String, val avatarUrl: String)

// ── 响应数据类 ──
data class ApiResponse(
    val code: Int = 0,
    val message: String = "",
    val data: UserData? = null
)

data class UserData(
    val userId: String = "",
    val nickname: String = "",
    val avatarUrl: String = "",
    val email: String = "",
    val phone: String = "",
    val token: String = ""
)

// ── 远程认证服务实现 ──
class RemoteAuthService(baseUrl: String) : AuthService {

    private var token: String = ""

    /** 获取当前token（供SyncManager使用） */
    fun getToken(): String = token

    private val api: AuthApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AuthApi::class.java)

    override suspend fun login(username: String, password: String): AuthResult {
        return try {
            val resp = api.login(LoginRequest(username, password))
            if (resp.code == 200 && resp.data != null) {
                token = resp.data.token
                AuthResult.Success(resp.data.toUserInfo())
            } else {
                AuthResult.Error(resp.message.ifBlank { "登录失败" })
            }
        } catch (e: Exception) {
            AuthResult.Error("网络错误：${e.localizedMessage}")
        }
    }

    override suspend fun register(username: String, password: String, nickname: String): AuthResult {
        return try {
            val resp = api.register(RegisterRequest(username, password, nickname))
            if (resp.code == 200 && resp.data != null) {
                token = resp.data.token
                AuthResult.Success(resp.data.toUserInfo())
            } else {
                AuthResult.Error(resp.message.ifBlank { "注册失败" })
            }
        } catch (e: Exception) {
            AuthResult.Error("网络错误：${e.localizedMessage}")
        }
    }

    override suspend fun logout(): Boolean {
        return try {
            api.logout("Bearer $token")
            token = ""
            true
        } catch (e: Exception) { true }
    }

    override suspend fun refreshUserInfo(userId: String): AuthResult {
        return try {
            val resp = api.getProfile("Bearer $token")
            if (resp.code == 200 && resp.data != null) {
                AuthResult.Success(resp.data.toUserInfo())
            } else {
                AuthResult.Error(resp.message)
            }
        } catch (e: Exception) {
            AuthResult.Error("网络错误：${e.localizedMessage}")
        }
    }

    override suspend fun updateProfile(userId: String, nickname: String, avatarUrl: String): AuthResult {
        return try {
            val resp = api.updateProfile("Bearer $token", UpdateProfileRequest(nickname, avatarUrl))
            if (resp.code == 200 && resp.data != null) {
                AuthResult.Success(resp.data.toUserInfo())
            } else {
                AuthResult.Error(resp.message)
            }
        } catch (e: Exception) {
            AuthResult.Error("网络错误：${e.localizedMessage}")
        }
    }

    private fun UserData.toUserInfo() = UserInfo(
        userId = userId,
        nickname = nickname,
        avatarUrl = avatarUrl,
        email = email,
        phone = phone
    )
}
