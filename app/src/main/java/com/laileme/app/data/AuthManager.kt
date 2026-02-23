package com.laileme.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 用户信息数据类
 */
data class UserInfo(
    val userId: String = "",
    val nickname: String = "",
    val avatarUrl: String = "",   // 头像URL，空字符串表示使用默认头像
    val email: String = "",
    val phone: String = ""
)

/**
 * 认证结果
 */
sealed class AuthResult {
    data class Success(val user: UserInfo) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * 网络认证服务接口 —— 后期接入网络服务时实现此接口
 *
 * 使用方法：
 * 1. 创建一个实现了 AuthService 的类（如 RemoteAuthService）
 * 2. 调用 AuthManager.setAuthService(remoteAuthService) 替换默认实现
 * 3. 所有登录/注册/登出逻辑将自动走网络
 */
interface AuthService {
    /** 登录 */
    suspend fun login(username: String, password: String): AuthResult

    /** 注册 */
    suspend fun register(username: String, password: String, nickname: String): AuthResult

    /** 登出 */
    suspend fun logout(): Boolean

    /** 刷新用户信息（如从服务器拉取最新资料） */
    suspend fun refreshUserInfo(userId: String): AuthResult

    /** 更新用户资料 */
    suspend fun updateProfile(userId: String, nickname: String, avatarUrl: String): AuthResult
}

/**
 * 默认离线认证服务 —— 仅本地存储，不联网
 */
class OfflineAuthService : AuthService {
    override suspend fun login(username: String, password: String): AuthResult {
        // 离线模式：只要用户名不为空即可"登录"
        return if (username.isNotBlank()) {
            AuthResult.Success(
                UserInfo(
                    userId = username,
                    nickname = username,
                    avatarUrl = ""
                )
            )
        } else {
            AuthResult.Error("用户名不能为空")
        }
    }

    override suspend fun register(username: String, password: String, nickname: String): AuthResult {
        return if (username.isNotBlank() && password.length >= 6) {
            AuthResult.Success(
                UserInfo(
                    userId = username,
                    nickname = nickname.ifBlank { username },
                    avatarUrl = ""
                )
            )
        } else {
            AuthResult.Error(if (username.isBlank()) "用户名不能为空" else "密码至少6位")
        }
    }

    override suspend fun logout(): Boolean = true

    override suspend fun refreshUserInfo(userId: String): AuthResult {
        return AuthResult.Error("离线模式不支持刷新")
    }

    override suspend fun updateProfile(userId: String, nickname: String, avatarUrl: String): AuthResult {
        return AuthResult.Success(UserInfo(userId = userId, nickname = nickname, avatarUrl = avatarUrl))
    }
}

/**
 * 认证管理器 —— 单例，管理登录状态
 */
object AuthManager {
    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context
    private var authService: AuthService = OfflineAuthService()

    private val _userState = MutableStateFlow<UserInfo?>(null)
    val userState: StateFlow<UserInfo?> = _userState.asStateFlow()

    val isLoggedIn: Boolean get() = _userState.value != null

    /** 初始化，在 Application 或 MainActivity 中调用 */
    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences("laileme_auth", Context.MODE_PRIVATE)
        // 恢复本地登录状态
        val savedUserId = prefs.getString("user_id", "") ?: ""
        if (savedUserId.isNotEmpty()) {
            _userState.value = UserInfo(
                userId = savedUserId,
                nickname = prefs.getString("nickname", savedUserId) ?: savedUserId,
                avatarUrl = prefs.getString("avatar_url", "") ?: "",
                email = prefs.getString("email", "") ?: "",
                phone = prefs.getString("phone", "") ?: ""
            )
            // 恢复token并启动自动同步
            val savedToken = prefs.getString("token", "") ?: ""
            if (savedToken.isNotEmpty()) {
                SyncManager.setToken(savedToken)
                SyncManager.startAutoSync(appContext)
            }
        }
    }

    /** 替换认证服务（接入网络时调用） */
    fun setAuthService(service: AuthService) {
        authService = service
    }

    /** 登录 */
    suspend fun login(username: String, password: String): AuthResult {
        val result = authService.login(username, password)
        if (result is AuthResult.Success) {
            saveUser(result.user)
            // 获取token用于同步
            if (authService is RemoteAuthService) {
                val token = (authService as RemoteAuthService).getToken()
                prefs.edit().putString("token", token).apply()
                SyncManager.setToken(token)
                if (::appContext.isInitialized) SyncManager.startAutoSync(appContext)
            }
        }
        return result
    }

    /** 注册 */
    suspend fun register(username: String, password: String, nickname: String): AuthResult {
        val result = authService.register(username, password, nickname)
        if (result is AuthResult.Success) {
            saveUser(result.user)
            if (authService is RemoteAuthService) {
                val token = (authService as RemoteAuthService).getToken()
                prefs.edit().putString("token", token).apply()
                SyncManager.setToken(token)
                if (::appContext.isInitialized) SyncManager.startAutoSync(appContext)
            }
        }
        return result
    }

    /** 登出 */
    suspend fun logout(): Boolean {
        val success = authService.logout()
        if (success) {
            SyncManager.stopAutoSync()
            SyncManager.setToken("")
            clearUser()
        }
        return success
    }

    /** 更新昵称 */
    suspend fun updateNickname(newNickname: String): AuthResult {
        val user = _userState.value ?: return AuthResult.Error("未登录")
        val result = authService.updateProfile(user.userId, newNickname, user.avatarUrl)
        if (result is AuthResult.Success) {
            saveUser(result.user)
        }
        return result
    }

    private fun saveUser(user: UserInfo) {
        _userState.value = user
        prefs.edit()
            .putString("user_id", user.userId)
            .putString("nickname", user.nickname)
            .putString("avatar_url", user.avatarUrl)
            .putString("email", user.email)
            .putString("phone", user.phone)
            .apply()
    }

    private fun clearUser() {
        _userState.value = null
        prefs.edit().clear().apply()  // 同时清除token
    }
}
