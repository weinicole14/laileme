package com.laileme.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 用户信息数据类
 */
data class UserInfo(
    val userId: String = "",
    val username: String = "",    // 用户名（登录账号）
    val uid: String = "",         // 格式化的UID，如00001
    val nickname: String = "",
    val avatarUrl: String = "",   // 头像URL，空字符串表示使用默认头像
    val email: String = "",
    val phone: String = "",
    val gender: String = "female" // 性别：female/male
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
    suspend fun register(username: String, password: String, nickname: String, gender: String = "female"): AuthResult

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

    override suspend fun register(username: String, password: String, nickname: String, gender: String): AuthResult {
        return if (username.isNotBlank() && password.length >= 6) {
            AuthResult.Success(
                UserInfo(
                    userId = username,
                    nickname = nickname.ifBlank { username },
                    avatarUrl = "",
                    gender = gender
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
                username = prefs.getString("username", "") ?: "",
                uid = prefs.getString("uid", "") ?: "",
                nickname = prefs.getString("nickname", savedUserId) ?: savedUserId,
                avatarUrl = prefs.getString("avatar_url", "") ?: "",
                email = prefs.getString("email", "") ?: "",
                phone = prefs.getString("phone", "") ?: "",
                gender = prefs.getString("gender", "female") ?: "female"
            )
            // 恢复token并启动自动同步
            val savedToken = prefs.getString("token", "") ?: ""
            if (savedToken.isNotEmpty()) {
                SyncManager.setToken(savedToken)
                // 同时恢复RemoteAuthService的token
                if (authService is RemoteAuthService) {
                    (authService as RemoteAuthService).restoreToken(savedToken)
                }
                SyncManager.startAutoSync(appContext)
                // 男性用户启动伴侣数据轮询
                if (_userState.value?.gender == "male") {
                    SyncManager.startPartnerPoll(appContext)
                }
            }
        }
    }

    /** 替换认证服务（接入网络时调用） */
    fun setAuthService(service: AuthService) {
        authService = service
        // 如果已初始化且有保存的token，恢复给新的认证服务
        if (::prefs.isInitialized && service is RemoteAuthService) {
            val savedToken = prefs.getString("token", "") ?: ""
            if (savedToken.isNotEmpty()) {
                service.restoreToken(savedToken)
                SyncManager.setToken(savedToken)
                // App重启时自动恢复数据和VIP状态
                if (::appContext.isInitialized && _userState.value != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // 刷新用户信息（确保gender等字段是最新的）
                            val refreshResult = authService.refreshUserInfo(_userState.value!!.userId)
                            if (refreshResult is AuthResult.Success) {
                                saveUser(refreshResult.user)
                                android.util.Log.i("AuthManager", "用户信息刷新成功: gender=${refreshResult.user.gender}")
                            }
                            syncVipStatusFromServer(savedToken)
                            SyncManager.downloadAndRestore(appContext)
                            android.util.Log.i("AuthManager", "App重启数据恢复成功")
                        } catch (e: Exception) {
                            android.util.Log.e("AuthManager", "App重启数据恢复失败", e)
                        }
                        SyncManager.startAutoSync(appContext)
                        // 男性用户启动伴侣数据轮询
                        if (_userState.value?.gender == "male") {
                            SyncManager.startPartnerPoll(appContext)
                        }
                    }
                }
            }
        }
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
                // 登录后从服务器同步VIP状态
                syncVipStatusFromServer(token)
                // 登录后先从服务器下载恢复数据，再启动自动上传同步
                if (::appContext.isInitialized) {
                    try {
                        SyncManager.downloadAndRestore(appContext)
                        android.util.Log.i("AuthManager", "登录后数据恢复成功")
                    } catch (e: Exception) {
                        android.util.Log.e("AuthManager", "登录后数据恢复失败", e)
                    }
                    SyncManager.startAutoSync(appContext)
                    // 男性用户启动伴侣数据轮询
                    if (result.user.gender == "male") {
                        SyncManager.startPartnerPoll(appContext)
                    }
                }
            }
        }
        return result
    }

    /** 注册 */
    suspend fun register(username: String, password: String, nickname: String, gender: String = "female"): AuthResult {
        val result = authService.register(username, password, nickname, gender)
        if (result is AuthResult.Success) {
            saveUser(result.user)
            if (authService is RemoteAuthService) {
                val token = (authService as RemoteAuthService).getToken()
                prefs.edit().putString("token", token).apply()
                SyncManager.setToken(token)
                // 注册后也同步一下VIP状态（虽然新用户一般不是VIP）
                syncVipStatusFromServer(token)
                if (::appContext.isInitialized) {
                    SyncManager.startAutoSync(appContext)
                    // 男性用户启动伴侣数据轮询
                    if (gender == "male") {
                        SyncManager.startPartnerPoll(appContext)
                    }
                }
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

    /** 更新头像URL（上传成功后调用） */
    suspend fun updateAvatarUrl(avatarUrl: String) {
        val user = _userState.value ?: return
        val updated = user.copy(avatarUrl = avatarUrl)
        _userState.value = updated
        prefs.edit().putString("avatar_url", avatarUrl).apply()
        // 同步到服务器用户资料
        try {
            authService.updateProfile(user.userId, user.nickname, avatarUrl)
        } catch (_: Exception) {}
    }

    /** 从服务器同步VIP状态到本地 */
    private suspend fun syncVipStatusFromServer(token: String) {
        try {
            val vipStatus = PaymentManager.checkVipStatus(token)
            if (::appContext.isInitialized) {
                PaymentManager.saveLocalVipStatus(
                    appContext,
                    vipStatus.isVip,
                    vipStatus.planType
                )
            }
        } catch (e: Exception) {
            // VIP状态同步失败不影响登录
            android.util.Log.e("AuthManager", "VIP状态同步失败", e)
        }
    }

    private fun saveUser(user: UserInfo) {
        _userState.value = user
        prefs.edit()
            .putString("user_id", user.userId)
            .putString("username", user.username)
            .putString("uid", user.uid)
            .putString("gender", user.gender)
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
