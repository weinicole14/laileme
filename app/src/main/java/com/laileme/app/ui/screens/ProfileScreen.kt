package com.laileme.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.laileme.app.data.AuthManager
import com.laileme.app.data.AuthResult
import com.laileme.app.data.InvitationManager
import com.laileme.app.data.PartnerInfo
import com.laileme.app.data.PartnerManager
import com.laileme.app.data.PartnerRequest
import com.laileme.app.data.PaymentManager
import com.laileme.app.data.SyncManager
import com.laileme.app.data.UserInfo
import com.laileme.app.ui.PeriodUiState
import com.laileme.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

// 伴侣信息缓存（避免页面重建时异步加载导致布局跳变）
private var cachedPartnerInfo: PartnerInfo? = null

// 页面状态枚举
private enum class ProfilePage { MAIN, SETTINGS, PROFILE_INFO, LOGIN, ABOUT, PREMIUM, INVITATION, PARTNER, PRIVACY_POLICY, SERVICE_AGREEMENT, INFO_COLLECTION }

@Composable
fun ProfileScreen(
    uiState: PeriodUiState,
    onSaveSettings: (Int, Int) -> Unit,
    onSaveMode: (String) -> Unit,
    onSubPageChanged: (Boolean) -> Unit = {}
) {
    var currentPage by remember { mutableStateOf(ProfilePage.MAIN) }
    var previousPage by remember { mutableStateOf(ProfilePage.MAIN) }
    // 记录登录成功后应跳转的页面（如从付费页跳转登录，登录后自动返回付费页）
    var pendingAfterLogin by remember { mutableStateOf<ProfilePage?>(null) }

    // 通知外部当前是否在子页面
    LaunchedEffect(currentPage) {
        onSubPageChanged(currentPage != ProfilePage.MAIN)
    }

    // 二级页面拦截系统返回键，返回主页面
    BackHandler(enabled = currentPage != ProfilePage.MAIN) {
        previousPage = currentPage
        
        // 如果是在三级页面（法律文本），或者其它从设置进来的页面，按返回键退回设置页
        if (currentPage == ProfilePage.PRIVACY_POLICY || 
            currentPage == ProfilePage.SERVICE_AGREEMENT || 
            currentPage == ProfilePage.INFO_COLLECTION) {
            currentPage = ProfilePage.SETTINGS
        } else {
            currentPage = ProfilePage.MAIN
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                val goingForward = targetState.ordinal > previousPage.ordinal
                if (goingForward) {
                    (slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = tween(300)
                    ) + fadeOut(tween(150)))
                } else {
                    (slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(
                        targetOffsetX = { it / 3 },
                        animationSpec = tween(300)
                    ) + fadeOut(tween(150)))
                }.using(SizeTransform(clip = false))
            },
            label = "profile_pages"
        ) { page ->
            when (page) {
                ProfilePage.MAIN -> ProfileContent(
                    onOpenSettings = {
                        previousPage = ProfilePage.MAIN
                        currentPage = ProfilePage.SETTINGS
                    },
                    onOpenProfile = {
                        previousPage = ProfilePage.MAIN
                        currentPage = ProfilePage.PROFILE_INFO
                    },
                    onOpenLogin = {
                        previousPage = ProfilePage.MAIN
                        currentPage = ProfilePage.LOGIN
                    },
                    onOpenAbout = {
                        previousPage = ProfilePage.MAIN
                        currentPage = ProfilePage.ABOUT
                    },
                    onOpenPremium = {
                        previousPage = ProfilePage.MAIN
                        currentPage = ProfilePage.PREMIUM
                    },
                onOpenInvitation = {
                    previousPage = ProfilePage.MAIN
                    currentPage = ProfilePage.INVITATION
                },
                onOpenPartner = {
                    previousPage = ProfilePage.MAIN
                    currentPage = ProfilePage.PARTNER
                }
            )
                ProfilePage.SETTINGS -> SettingsContent(
                    uiState = uiState,
                    onSaveSettings = onSaveSettings,
                    onSaveMode = onSaveMode,
                    onBack = {
                        previousPage = ProfilePage.SETTINGS
                        currentPage = ProfilePage.MAIN
                    },
                    onOpenPrivacyPolicy = {
                        previousPage = ProfilePage.SETTINGS
                        currentPage = ProfilePage.PRIVACY_POLICY
                    },
                    onOpenServiceAgreement = {
                        previousPage = ProfilePage.SETTINGS
                        currentPage = ProfilePage.SERVICE_AGREEMENT
                    },
                    onOpenInfoCollection = {
                        previousPage = ProfilePage.SETTINGS
                        currentPage = ProfilePage.INFO_COLLECTION
                    }
                )
                ProfilePage.PROFILE_INFO -> ProfileInfoContent(
                    onBack = {
                        previousPage = ProfilePage.PROFILE_INFO
                        currentPage = ProfilePage.MAIN
                    }
                )
                ProfilePage.LOGIN -> LoginScreen(
                    onBack = {
                        pendingAfterLogin = null
                        previousPage = ProfilePage.LOGIN
                        currentPage = ProfilePage.MAIN
                    },
                    onLoginSuccess = {
                        val target = pendingAfterLogin
                        pendingAfterLogin = null
                        previousPage = ProfilePage.LOGIN
                        currentPage = target ?: ProfilePage.MAIN
                    }
                )
                ProfilePage.ABOUT -> AboutContent(
                    onBack = {
                        previousPage = ProfilePage.ABOUT
                        currentPage = ProfilePage.MAIN
                    }
                )
                ProfilePage.PREMIUM -> PremiumContent(
                    onBack = {
                        previousPage = ProfilePage.PREMIUM
                        currentPage = ProfilePage.MAIN
                    },
                    onOpenLogin = {
                        pendingAfterLogin = ProfilePage.PREMIUM
                        previousPage = ProfilePage.PREMIUM
                        currentPage = ProfilePage.LOGIN
                    }
                )
                ProfilePage.INVITATION -> InvitationContent(
                    onBack = {
                        previousPage = ProfilePage.INVITATION
                        currentPage = ProfilePage.MAIN
                    },
                    onOpenLogin = {
                        pendingAfterLogin = ProfilePage.INVITATION
                        previousPage = ProfilePage.INVITATION
                        currentPage = ProfilePage.LOGIN
                    }
                )
                ProfilePage.PARTNER -> PartnerContent(
                    onBack = {
                        previousPage = ProfilePage.PARTNER
                        currentPage = ProfilePage.MAIN
                    }
                )
                ProfilePage.PRIVACY_POLICY -> WebViewPage(
                    title = "隐私政策",
                    url = "file:///android_asset/privacy/privacy_policy.html",
                    onBack = {
                        previousPage = ProfilePage.PRIVACY_POLICY
                        currentPage = ProfilePage.SETTINGS
                    }
                )
                ProfilePage.SERVICE_AGREEMENT -> WebViewPage(
                    title = "服务协议",
                    url = "file:///android_asset/privacy/service_agreement.html",
                    onBack = {
                        previousPage = ProfilePage.SERVICE_AGREEMENT
                        currentPage = ProfilePage.SETTINGS
                    }
                )
                ProfilePage.INFO_COLLECTION -> WebViewPage(
                    title = "个人信息收集清单",
                    url = "file:///android_asset/privacy/personal_info_list.html",
                    onBack = {
                        previousPage = ProfilePage.INFO_COLLECTION
                        currentPage = ProfilePage.SETTINGS
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenLogin: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenPremium: () -> Unit = {},
    onOpenInvitation: () -> Unit = {},
    onOpenPartner: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("laileme_profile", Context.MODE_PRIVATE) }
    val localNickname = prefs.getString("nickname", "") ?: ""

    // 监听登录状态
    val user by AuthManager.userState.collectAsState()
    val isLoggedIn = user != null
    val displayName = when {
        isLoggedIn -> user?.nickname ?: "用户"
        localNickname.isNotEmpty() -> localNickname
        else -> "未登录"
    }

    val scope = rememberCoroutineScope()

    // 头像上传相关
    val avatarUrl = user?.avatarUrl ?: ""
    val fullAvatarUrl = if (avatarUrl.isNotBlank() && !avatarUrl.startsWith("http"))
        "http://47.123.5.171:8080$avatarUrl" else avatarUrl
    var isUploadingAvatar by remember { mutableStateOf(false) }
    val isVip = remember(user) { PaymentManager.isLocalVip(context) }

    // 伴侣信息（用于双头像显示）— 使用文件级缓存避免页面重建时布局跳变
    var partnerInfoForAvatar by remember { mutableStateOf(cachedPartnerInfo) }
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            try {
                val token = context.getSharedPreferences("laileme_auth", Context.MODE_PRIVATE)
                    .getString("token", "") ?: ""
                if (token.isNotEmpty()) {
                    val info = PartnerManager.getPartnerInfo(token)
                    if (info.bound) {
                        partnerInfoForAvatar = info
                        cachedPartnerInfo = info
                        // 缓存到SP供小部件读取（含昵称、头像、性别）
                        context.getSharedPreferences("laileme_partner_cache", Context.MODE_PRIVATE).edit()
                            .putBoolean("bound", true)
                            .putLong("boundAt", info.boundAt)
                            .putString("partnerNickname", info.partnerNickname)
                            .putString("partnerAvatarUrl", info.partnerAvatarUrl)
                            .putString("partnerGender", info.partnerGender)
                            .apply()
                        // 刷新小部件
                        context.sendBroadcast(Intent(com.laileme.app.widget.DashboardWidgetProvider.ACTION_REFRESH).setPackage(context.packageName))
                    } else {
                        partnerInfoForAvatar = null
                        cachedPartnerInfo = null
                        context.getSharedPreferences("laileme_partner_cache", Context.MODE_PRIVATE).edit()
                            .putBoolean("bound", false)
                            .putLong("boundAt", 0L)
                            .apply()
                    }
                }
            } catch (_: Exception) {}
        } else {
            partnerInfoForAvatar = null
            cachedPartnerInfo = null
        }
    }

    // 昵称编辑弹窗
    var showNicknameDialog by remember { mutableStateOf(false) }
    var editNickname by remember { mutableStateOf("") }
    var isUpdatingNickname by remember { mutableStateOf(false) }

    // 退出登录确认弹窗
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && isLoggedIn) {
            isUploadingAvatar = true
            scope.launch {
                try {
                    val token = context.getSharedPreferences("laileme_auth", Context.MODE_PRIVATE)
                        .getString("token", "") ?: ""
                    val result = uploadAvatar(context, token, uri)
                    if (result != null) {
                        // 更新本地用户信息
                        AuthManager.updateAvatarUrl(result)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProfileScreen", "头像上传失败", e)
                } finally {
                    isUploadingAvatar = false
                }
            }
        }
    }

    // 昵称编辑弹窗
    if (showNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editNickname.isNotBlank()) {
                            isUpdatingNickname = true
                            scope.launch {
                                val result = AuthManager.updateNickname(editNickname.trim())
                                isUpdatingNickname = false
                                if (result is AuthResult.Success) {
                                    showNicknameDialog = false
                                }
                            }
                        }
                    },
                    enabled = !isUpdatingNickname && editNickname.isNotBlank()
                ) {
                    if (isUpdatingNickname) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryPink)
                    } else {
                        Text("保存", color = PrimaryPink, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showNicknameDialog = false }) {
                    Text("取消", color = TextHint)
                }
            },
            title = { Text("修改昵称", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editNickname,
                    onValueChange = { if (it.length <= 20) editNickname = it },
                    label = { Text("昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPink,
                        cursorColor = PrimaryPink
                    )
                )
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
            .padding(bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        // 头像区域 — 绑定伴侣时显示双头像+爱心+名字
        if (isLoggedIn && partnerInfoForAvatar != null) {
            // ── 双头像模式：我 ♡ 伴侣 ──
            val pAvatar = partnerInfoForAvatar!!.partnerAvatarUrl
            val pGender = partnerInfoForAvatar!!.partnerGender
            val partnerAvatarFull = if (pAvatar.isNotBlank() && !pAvatar.startsWith("http"))
                "http://47.123.5.171:8080$pAvatar" else pAvatar
            val partnerName = partnerInfoForAvatar!!.partnerNickname.ifEmpty { "伴侣" }

            // ── 双头像展开动画 ──
            // 延迟启动，等页面切换动画完成后再展开
            var animStarted by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(300)
                animStarted = true
            }

            val spreadProgress by animateFloatAsState(
                targetValue = if (animStarted) 1f else 0f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "spread"
            )
            val heartAlpha by animateFloatAsState(
                targetValue = if (animStarted) 1f else 0f,
                animationSpec = tween(durationMillis = 400, delayMillis = 350, easing = FastOutSlowInEasing),
                label = "heartAlpha"
            )
            val heartScale by animateFloatAsState(
                targetValue = if (animStarted) 1f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "heartScale"
            )
            // 名字淡入
            val nameAlpha by animateFloatAsState(
                targetValue = if (animStarted) 1f else 0f,
                animationSpec = tween(durationMillis = 300, delayMillis = 500),
                label = "nameAlpha"
            )

            // 展开偏移量（从中心往两侧各移动 spreadOffset）
            val spreadOffset = 56.dp * spreadProgress

            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // 我的头像+名字（向左偏移）
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(x = -spreadOffset)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(PrimaryPink.copy(alpha = 0.15f))
                                .border(2.dp, PrimaryPink.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (fullAvatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(fullAvatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "我的头像",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = PrimaryPink
                                )
                            }
                            if (isUploadingAvatar) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                }
                            }
                        }
                        // 相机图标
                        if (!isUploadingAvatar) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryPink)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .alpha(nameAlpha)
                            .clickable {
                                editNickname = displayName
                                showNicknameDialog = true
                            }
                    ) {
                        Text(
                            text = displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "编辑昵称",
                            modifier = Modifier.size(11.dp),
                            tint = TextHint
                        )
                    }
                }

                // 爱心（中间弹出）
                Icon(
                    imageVector = Icons.Outlined.Favorite,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .alpha(heartAlpha)
                        .scale(heartScale),
                    tint = PrimaryPink
                )

                // 伴侣头像+名字（向右偏移）
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(x = spreadOffset)
                ) {
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pGender == "male") Color(0xFF4FC3F7).copy(alpha = 0.15f)
                                    else PrimaryPink.copy(alpha = 0.15f)
                                )
                                .border(
                                    2.dp,
                                    if (pGender == "male") Color(0xFF4FC3F7).copy(alpha = 0.3f)
                                    else PrimaryPink.copy(alpha = 0.3f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (partnerAvatarFull.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(partnerAvatarFull)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "伴侣头像",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = if (pGender == "male") Icons.Outlined.Male else Icons.Outlined.Female,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = if (pGender == "male") Color(0xFF4FC3F7) else PrimaryPink
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = partnerName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        maxLines = 1,
                        modifier = Modifier.alpha(nameAlpha)
                    )
                }
            }
        } else {
            // ── 单头像模式（未绑定或未登录）──
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clickable {
                        if (isLoggedIn) imagePickerLauncher.launch("image/*")
                        else onOpenLogin()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLoggedIn) PrimaryPink.copy(alpha = 0.2f)
                            else Color(0xFFE0E0E0).copy(alpha = 0.4f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoggedIn && fullAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(fullAvatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "头像",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (isLoggedIn) Icons.Outlined.FavoriteBorder else Icons.Outlined.PersonOutline,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = if (isLoggedIn) PrimaryPink else TextHint
                        )
                    }
                    if (isUploadingAvatar) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        }
                    }
                }
                if (isLoggedIn && !isUploadingAvatar) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-6).dp, y = (-6).dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(PrimaryPink)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = "更换头像", modifier = Modifier.size(16.dp), tint = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoggedIn) {
            if (partnerInfoForAvatar == null) {
                // 未绑定伴侣：显示大昵称（可点击编辑）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        editNickname = displayName
                        showNicknameDialog = true
                    }
                ) {
                    Text(
                        text = displayName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "编辑昵称",
                        modifier = Modifier.size(16.dp),
                        tint = TextHint
                    )
                }
            }
            // ── 相守天数（仅绑定伴侣时显示）──
            if (partnerInfoForAvatar != null && partnerInfoForAvatar!!.boundAt > 0) {
                val togetherDays = remember(partnerInfoForAvatar!!.boundAt) {
                    val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = partnerInfoForAvatar!!.boundAt }
                    val cal2 = java.util.Calendar.getInstance()
                    // 按日历天数计算：今天的年日 - 绑定那天的年日 + 1
                    val day1 = cal1.get(java.util.Calendar.YEAR) * 366L + cal1.get(java.util.Calendar.DAY_OF_YEAR)
                    val day2 = cal2.get(java.util.Calendar.YEAR) * 366L + cal2.get(java.util.Calendar.DAY_OF_YEAR)
                    (day2 - day1).toInt() + 1 // 绑定当天算第1天
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = PrimaryPink
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "相守第 $togetherDays 天",
                        fontSize = 17.sp,
                        color = PrimaryPink,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (user?.uid?.isNotEmpty() == true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "UID: ${user?.uid}",
                        fontSize = 12.sp,
                        color = TextHint
                    )
                    if (isVip) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFD700))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("VIP", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF8B4513))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = "记录每一天的美好",
                fontSize = 14.sp,
                color = TextSecondary
            )
        } else {
            // 未登录：显示登录按钮
            Text(
                text = "点击登录",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPink,
                modifier = Modifier.clickable { onOpenLogin() }
            )
            Text(
                text = "登录后可同步数据",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 小兔礼盒
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable(
                    onClick = onOpenPremium,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF0F5)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp,
                pressedElevation = 4.dp,
                focusedElevation = 4.dp,
                hoveredElevation = 4.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryPink.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CardGiftcard,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = PrimaryPink
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
text = "小兔礼赠",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPink
                    )
                    Text(
                        text = "解锁更多功能，支持开发者",
                        fontSize = 11.sp,
                        color = TextHint
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryPink)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "GO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 个人档案入口（男性隐藏）
        val profileGender = user?.gender ?: "female"
        if (profileGender != "male") {
            ProfileMenuCard(
                icon = Icons.Outlined.Person,
                title = "个人档案",
                subtitle = "生日、身高、体重、血型",
                onClick = onOpenProfile
            )
        }

        // 设置入口
        ProfileMenuCard(
            icon = Icons.Outlined.Settings,
            title = "设置",
            subtitle = "经期设置、通知、外观",
            onClick = onOpenSettings
        )

            // 我的伴侣
            ProfileMenuCard(
                icon = Icons.Outlined.Favorite,
                title = "我的伴侣",
                subtitle = "绑定伴侣，关心Ta的每一天",
                onClick = onOpenPartner
            )

            // 邀请有礼
            ProfileMenuCard(
                icon = Icons.Outlined.CardGiftcard,
                title = "邀请有礼",
                subtitle = "邀请好友，双方获得VIP奖励",
                onClick = onOpenInvitation
            )

        // 关于我们
        ProfileMenuCard(
            icon = Icons.Outlined.Info,
            title = "关于我们",
            subtitle = "版本信息、开发团队",
            onClick = onOpenAbout
        )

        // 已登录时显示退出登录
        if (isLoggedIn) {
            ProfileMenuCard(
                icon = Icons.Outlined.Logout,
                title = "退出登录",
                subtitle = "当前账号：${user?.username ?: ""}",
                onClick = { showLogoutConfirmDialog = true }
            )
        }
    }

    // 退出登录确认弹窗
    if (showLogoutConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.Outlined.Logout,
                    contentDescription = null,
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "确认退出登录？",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1A1A2E)
                )
            },
            text = {
                Text(
                    "退出后需要重新登录才能使用同步等功能",
                    fontSize = 13.sp,
                    color = Color(0xFF666680),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirmDialog = false
                        scope.launch { AuthManager.logout() }
                    }
                ) {
                    Text("退出", color = Color(0xFFE91E63), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("取消", color = Color(0xFF666680))
                }
            }
        )
    }
}

@Composable
private fun ProfileMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryPink.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = PrimaryPink
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextHint
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = TextHint
            )
        }
    }
}

// ──────────────── 个人档案页面 ────────────────

@Composable
private fun ProfileInfoContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("laileme_profile", Context.MODE_PRIVATE) }

    var nickname by remember { mutableStateOf(prefs.getString("nickname", "") ?: "") }
    var birthYear by remember { mutableStateOf(prefs.getString("birth_year", "") ?: "") }
    var birthMonth by remember { mutableStateOf(prefs.getString("birth_month", "") ?: "") }
    var birthDay by remember { mutableStateOf(prefs.getString("birth_day", "") ?: "") }
    var height by remember { mutableStateOf(prefs.getString("height", "") ?: "") }
    var weight by remember { mutableStateOf(prefs.getString("weight", "") ?: "") }
    var bloodType by remember { mutableStateOf(prefs.getString("blood_type", "") ?: "") }
    var saved by remember { mutableStateOf(false) }

    // 计算年龄
    val age = remember(birthYear, birthMonth, birthDay) {
        try {
            val y = birthYear.toIntOrNull() ?: return@remember null
            val m = birthMonth.toIntOrNull() ?: return@remember null
            val d = birthDay.toIntOrNull() ?: return@remember null
            val now = Calendar.getInstance()
            var a = now.get(Calendar.YEAR) - y
            if (now.get(Calendar.MONTH) + 1 < m || (now.get(Calendar.MONTH) + 1 == m && now.get(Calendar.DAY_OF_MONTH) < d)) {
                a--
            }
            if (a >= 0) a else null
        } catch (_: Exception) { null }
    }

    // 计算BMI
    val bmi = remember(height, weight) {
        try {
            val h = height.toFloatOrNull() ?: return@remember null
            val w = weight.toFloatOrNull() ?: return@remember null
            if (h > 0 && w > 0) {
                val hm = h / 100f
                w / (hm * hm)
            } else null
        } catch (_: Exception) { null }
    }

    val bmiCategory = remember(bmi) {
        when {
            bmi == null -> null
            bmi < 18.5f -> "偏瘦"
            bmi < 24f -> "正常"
            bmi < 28f -> "偏胖"
            else -> "肥胖"
        }
    }

    val bmiColor = remember(bmiCategory) {
        when (bmiCategory) {
            "偏瘦" -> AccentBlue
            "正常" -> AccentTeal
            "偏胖" -> AccentOrange
            "肥胖" -> PeriodRed
            else -> TextHint
        }
    }

    val bmiAdvice = remember(bmiCategory) {
        when (bmiCategory) {
            "偏瘦" -> "建议适当增加营养摄入，保证充足蛋白质和碳水化合物，有助于维持健康体重和正常月经周期~"
            "正常" -> "体重很健康哦！继续保持均衡饮食和适度运动，身体棒棒的~"
            "偏胖" -> "建议适当控制饮食，增加有氧运动。体重过重可能影响月经规律，保持健康体重很重要哦~"
            "肥胖" -> "建议咨询医生制定科学的减重计划。肥胖可能导致月经不调等问题，健康减重对经期调理很有帮助~"
            else -> null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryPink.copy(alpha = 0.1f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(20.dp),
                    tint = PrimaryPink
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "个人档案",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // 可滚动内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
                .padding(bottom = 80.dp)
        ) {
            // ── 基本信息 ──
            ProfileSectionTitle("基本信息")

            ProfileTextField(
                label = "昵称",
                value = nickname,
                onValueChange = { nickname = it; saved = false },
                placeholder = "输入你的昵称"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 生日（年月日）
            Text("生日", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ProfileTextField(
                    label = "",
                    value = birthYear,
                    onValueChange = { birthYear = it.filter { c -> c.isDigit() }.take(4); saved = false },
                    placeholder = "年",
                    modifier = Modifier.weight(1.2f),
                    isNumber = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                ProfileTextField(
                    label = "",
                    value = birthMonth,
                    onValueChange = { birthMonth = it.filter { c -> c.isDigit() }.take(2); saved = false },
                    placeholder = "月",
                    modifier = Modifier.weight(1f),
                    isNumber = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                ProfileTextField(
                    label = "",
                    value = birthDay,
                    onValueChange = { birthDay = it.filter { c -> c.isDigit() }.take(2); saved = false },
                    placeholder = "日",
                    modifier = Modifier.weight(1f),
                    isNumber = true
                )
            }

            if (age != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentTeal.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Cake,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AccentTeal
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${age}岁",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentTeal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 身体数据 ──
            ProfileSectionTitle("身体数据")

            Row(modifier = Modifier.fillMaxWidth()) {
                ProfileTextField(
                    label = "身高 (cm)",
                    value = height,
                    onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' }.take(5); saved = false },
                    placeholder = "160",
                    modifier = Modifier.weight(1f),
                    isNumber = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                ProfileTextField(
                    label = "体重 (kg)",
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' }.take(5); saved = false },
                    placeholder = "50",
                    modifier = Modifier.weight(1f),
                    isNumber = true
                )
            }

            // BMI 显示
            if (bmi != null && bmiCategory != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bmiColor.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "BMI",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = bmiColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "%.1f".format(bmi),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = bmiColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bmiColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    bmiCategory,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bmiColor
                                )
                            }
                        }
                        if (bmiAdvice != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                bmiAdvice,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 血型 ──
            ProfileSectionTitle("血型")

            val bloodTypes = listOf("A", "B", "AB", "O", "未知")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bloodTypes.forEach { type ->
                    val isSelected = bloodType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) PrimaryPink.copy(alpha = 0.15f) else Color.White)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) PrimaryPink else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { bloodType = type; saved = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            type,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PrimaryPink else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 保存按钮
            Button(
                onClick = {
                    prefs.edit()
                        .putString("nickname", nickname)
                        .putString("birth_year", birthYear)
                        .putString("birth_month", birthMonth)
                        .putString("birth_day", birthDay)
                        .putString("height", height)
                        .putString("weight", weight)
                        .putString("blood_type", bloodType)
                        .apply()
                    saved = true
                    com.laileme.app.data.SyncManager.triggerImmediateSync()
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
            ) {
                Icon(
                    imageVector = if (saved) Icons.Outlined.CheckCircle else Icons.Outlined.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (saved) "已保存 ✓" else "保存档案",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = PrimaryPink,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 4.dp)
    )
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isNumber: Boolean = false
) {
    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(
                label, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = TextPrimary, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 13.sp, color = TextHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPink,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        )
    }
}

// ──────────────── 关于我们页面 ────────────────

@Composable
private fun AboutContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryPink.copy(alpha = 0.1f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(20.dp),
                    tint = PrimaryPink
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "关于我们",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // 可滚动内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // App Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(PrimaryPink.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = PrimaryPink
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "来了么",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPink
            )
            Text(
                text = "LaiLeMe",
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 版本号
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AccentTeal.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "版本 beta 1.6",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccentTeal
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 应用介绍卡片
            AboutCard(
                icon = Icons.Outlined.FavoriteBorder,
                title = "关于来了么",
                content = "「来了么」是一款专为女性设计的健康生活助手。\n\n" +
                    "我们致力于帮助你轻松记录经期、睡眠、心情等日常健康数据，" +
                    "通过科学的数据分析，让你更了解自己的身体，享受每一天的生活。\n\n" +
                    "每一次记录，都是对自己的温柔关爱。"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 功能特色
            AboutCard(
                icon = Icons.Outlined.Stars,
                title = "功能特色",
                content = "经期记录 — 智能预测下次经期\n" +
                    "睡眠管理 — 记录入睡起床时间\n" +
                    "心情日记 — 记录每天的心情\n" +
                    "数据统计 — 直观图表展示趋势\n" +
                    "隐私保护 — 你的数据安全第一"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 开发团队
            AboutCard(
                icon = Icons.Outlined.Group,
                title = "开发团队",
                content = "来了么由热爱生活的独立开发者用心打造。\n\n" +
                    "我们相信科技可以让生活更美好，" +
                    "希望这款小小的App能成为你日常生活中的贴心伙伴。"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 联系我们
            AboutCard(
                icon = Icons.Outlined.Email,
                title = "联系我们",
                content = "如果你有任何建议、问题或者想跟我们说的话，\n" +
                    "欢迎通过以下方式联系：\n\n" +
                    "邮箱：postmaster@weinicole.cn\n" +
                    "官网：weinicole.cn"
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 底部版权信息
            Text(
                text = "Made with Love",
                fontSize = 14.sp,
                color = PrimaryPink,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "© 2025 来了么 All Rights Reserved",
                fontSize = 11.sp,
                color = TextHint
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun AboutCard(
    icon: ImageVector,
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryPink.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = PrimaryPink
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = content,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

// ──────────────── 邀请有礼页面 ────────────────

@Composable
private fun InvitationContent(onBack: () -> Unit, onOpenLogin: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val user by AuthManager.userState.collectAsState()
    val isLoggedIn = user != null

    // 邀请信息
    var myCode by remember { mutableStateOf("") }
    var invitedCount by remember { mutableIntStateOf(0) }
    var rewardDays by remember { mutableIntStateOf(0) }
    var usedCode by remember { mutableStateOf<String?>(null) }
    var isLoadingInfo by remember { mutableStateOf(false) }

    // 使用邀请码
    var inputCode by remember { mutableStateOf("") }
    var isRedeeming by remember { mutableStateOf(false) }

    // 弹窗
    var showSuccessDialog by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf<String?>(null) }


    // 复制到剪贴板
    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("邀请码", text))
    }

    // 获取token
    fun getToken(): String {
        return try {
            context.getSharedPreferences("laileme_auth", Context.MODE_PRIVATE)
                .getString("token", "") ?: ""
        } catch (_: Exception) { "" }
    }

    // 加载我的邀请信息
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            isLoadingInfo = true
            val token = getToken()
            if (token.isNotBlank()) {
                val info = InvitationManager.getMyInvitation(token)
                myCode = info.myCode
                invitedCount = info.invitedCount
                rewardDays = info.rewardDays
                usedCode = info.usedCode
            }
            isLoadingInfo = false
        }
    }

    // 成功弹窗
    if (showSuccessDialog != null) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = null },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = null }) {
                    Text("太好了～", color = PrimaryPink, fontWeight = FontWeight.Bold)
                }
            },
            icon = { Icon(Icons.Outlined.CheckCircle, null, tint = AccentTeal, modifier = Modifier.size(48.dp)) },
            title = { Text("兑换成功！", fontWeight = FontWeight.Bold) },
            text = { Text(showSuccessDialog ?: "", color = TextSecondary) },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 错误弹窗
    if (showErrorDialog != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = null },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = null }) {
                    Text("知道了", color = PrimaryPink)
                }
            },
            icon = { Icon(Icons.Outlined.ErrorOutline, null, tint = PeriodRed, modifier = Modifier.size(48.dp)) },
            title = { Text("提示", fontWeight = FontWeight.Bold) },
            text = { Text(showErrorDialog ?: "", color = TextSecondary) },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryPink.copy(alpha = 0.1f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(20.dp),
                    tint = PrimaryPink
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "邀请有礼",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 邀请图标
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF0F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CardGiftcard,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = PrimaryPink
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("分享来了么 · 一起记录生活", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryPink)
            Spacer(modifier = Modifier.height(4.dp))
            Text("把专属邀请码分享给闺蜜，你和她都能获得VIP体验哦～", fontSize = 12.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(24.dp))

            if (!isLoggedIn) {
                // 未登录提示
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.PersonOutline, null, modifier = Modifier.size(40.dp), tint = TextHint)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("登录后查看你的专属邀请码", fontSize = 14.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onOpenLogin,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
                        ) {
                            Text("去登录", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            } else {
                // ── 我的邀请码 ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("我的邀请码", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isLoadingInfo) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = PrimaryPink,
                                strokeWidth = 2.dp
                            )
                        } else if (myCode.isNotBlank()) {
                            // 邀请码显示
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimaryPink.copy(alpha = 0.08f))
                                    .border(1.dp, PrimaryPink.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 24.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = myCode,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryPink,
                                    letterSpacing = 3.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 复制按钮
                            Button(
                                onClick = {
                                    copyToClipboard(myCode)
                                    showSuccessDialog = "邀请码已复制到剪贴板，快分享给好友吧～"
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
                            ) {
                                Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("复制邀请码", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            Text("暂无邀请码", fontSize = 14.sp, color = TextHint)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 邀请统计 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("$invitedCount", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrimaryPink)
                            Text("已邀请人数", fontSize = 11.sp, color = TextHint)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("${rewardDays}天", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AccentTeal)
                            Text("累计获得VIP", fontSize = 11.sp, color = TextHint)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── 使用邀请码 ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "使用邀请码",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (usedCode != null) {
                            // 已使用过邀请码
                            Text(
                                "你已使用过邀请码：$usedCode",
                                fontSize = 13.sp,
                                color = AccentTeal
                            )
                        } else {
                            Text(
                                "输入好友的邀请码，双方各获得VIP奖励",
                                fontSize = 12.sp,
                                color = TextHint
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = inputCode,
                                onValueChange = { inputCode = it.uppercase().trim() },
                                placeholder = { Text("请输入邀请码", color = TextHint) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryPink,
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    cursorColor = PrimaryPink
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (inputCode.isBlank()) {
                                        showErrorDialog = "请输入邀请码"
                                        return@Button
                                    }
                                    isRedeeming = true
                                    scope.launch {
                                        val token = getToken()
                                        if (token.isBlank()) {
                                            isRedeeming = false
                                            showErrorDialog = "登录信息已过期，请重新登录"
                                            return@launch
                                        }
                                        val result = InvitationManager.useInvitationCode(token, inputCode)
                                        isRedeeming = false
                                        if (result.success) {
                                            usedCode = inputCode
                                            // 刷新VIP状态
                                            PaymentManager.saveLocalVipStatus(context, true, "invitation")
                                            showSuccessDialog = result.message + if (result.rewardDays > 0) "\n获得${result.rewardDays}天VIP奖励！" else ""
                                            // 重新获取邀请信息
                                            val info = InvitationManager.getMyInvitation(token)
                                            rewardDays = info.rewardDays
                                        } else {
                                            showErrorDialog = result.message
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                                enabled = !isRedeeming
                            ) {
                                if (isRedeeming) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("兑换中...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                } else {
                                    Text("兑换邀请码", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── 规则说明 ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("邀请规则", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryPink)
                        Spacer(modifier = Modifier.height(8.dp))
                        InvitationRule("每位用户拥有唯一的专属邀请码")
                        InvitationRule("每成功邀请1位好友，即可获得1个月VIP")
                        InvitationRule("累计邀请满10人，即可获得永久VIP")
                        InvitationRule("每位用户只能使用一次邀请码")
                        InvitationRule("不能使用自己的邀请码")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "本活动最终解释权归开发者所有",
                            fontSize = 10.sp,
                            color = TextHint
                        )
                    }
                }

            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun InvitationRule(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(PrimaryPink.copy(alpha = 0.5f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
    }
}

// ──────────────── 小兔礼盒 付费页面 ────────────────

@Composable
private fun PremiumContent(onBack: () -> Unit, onOpenLogin: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPlan by remember { mutableIntStateOf(0) } // 0=月付, 1=年付, 2=永久
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf<String?>(null) }

    // 监听登录状态
    val user by AuthManager.userState.collectAsState()
    val isLoggedIn = user != null
    // 本地VIP状态
    val isVip = remember(user) { PaymentManager.isLocalVip(context) }

    // 成功弹窗
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false; onBack() }) {
                    Text("好的～", color = PrimaryPink, fontWeight = FontWeight.Bold)
                }
            },
            icon = { Icon(Icons.Outlined.CheckCircle, null, tint = AccentTeal, modifier = Modifier.size(48.dp)) },
            title = { Text("开通成功！", fontWeight = FontWeight.Bold) },
            text = { Text("感谢你的支持～小兔礼赠已为你开启全部权益 ♡", color = TextSecondary) },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 错误弹窗
    if (showErrorDialog != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = null },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = null }) {
                    Text("知道了", color = PrimaryPink)
                }
            },
            icon = { Icon(Icons.Outlined.ErrorOutline, null, tint = PeriodRed, modifier = Modifier.size(48.dp)) },
            title = { Text("开通失败", fontWeight = FontWeight.Bold) },
            text = { Text(showErrorDialog ?: "", color = TextSecondary) },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryPink.copy(alpha = 0.1f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(20.dp),
                    tint = PrimaryPink
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
            text = "小兔礼赠",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            // VIP状态徽章
            if (isVip) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentTeal.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("已开通", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentTeal)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // 小兔图标
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF0F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CardGiftcard,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = PrimaryPink
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
            text = "小兔礼赠",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPink
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "请开发者喝杯咖啡吧~",
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "你的每一份支持，都是我们继续前行的动力",
                fontSize = 12.sp,
                color = TextHint
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 三个价格方案
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PricePlanCard(
                    title = "月卡",
                    price = "6",
                    unit = "元/月",
                    desc = "尝鲜体验",
                    isSelected = selectedPlan == 0,
                    onClick = { selectedPlan = 0 },
                    modifier = Modifier.weight(1f)
                )
                PricePlanCard(
                    title = "年卡",
                    price = "28",
                    unit = "元/年",
                    desc = "超值推荐",
                    isSelected = selectedPlan == 1,
                    onClick = { selectedPlan = 1 },
                    modifier = Modifier.weight(1f),
                    badge = "省61%"
                )
                PricePlanCard(
                    title = "永久",
                    price = "48",
                    unit = "元/永久",
                    desc = "一次拥有",
                    isSelected = selectedPlan == 2,
                    onClick = { selectedPlan = 2 },
                    modifier = Modifier.weight(1f),
                    badge = "最划算"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 购买按钮（未登录显示"登录后开通"，已登录显示"立即开通"）
            Button(
                onClick = {
                    if (!isLoggedIn) {
                        // 未登录 → 跳转登录，登录成功后自动返回此页面
                        onOpenLogin()
                    } else if (isVip) {
                        // 已是VIP
                        showSuccessDialog = true
                    } else {
                        // 已登录 → 调用付费接口
                        isLoading = true
                        val planTypeStr = when (selectedPlan) {
                            0 -> "monthly"
                            1 -> "yearly"
                            else -> "lifetime"
                        }
                        scope.launch {
                            val token = try {
                                val prefs = context.getSharedPreferences("laileme_auth", Context.MODE_PRIVATE)
                                prefs.getString("token", "") ?: ""
                            } catch (_: Exception) { "" }

                            if (token.isBlank()) {
                                isLoading = false
                                showErrorDialog = "登录信息已过期，请重新登录"
                                return@launch
                            }

                            val result = PaymentManager.createOrder(planTypeStr, token)
                            isLoading = false
                            if (result.success) {
                                PaymentManager.saveLocalVipStatus(context, true, planTypeStr)
                                // 开通VIP后启动自动同步
                                SyncManager.startAutoSync(context)
                                showSuccessDialog = true
                            } else {
                                showErrorDialog = result.message
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isVip) AccentTeal else PrimaryPink
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("处理中...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else if (!isLoggedIn) {
                    Icon(Icons.Outlined.Login, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("登录后开通", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else if (isVip) {
                    Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("已开通 · 感谢支持 ♡", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Text(
                        text = when (selectedPlan) {
                            0 -> "立即开通 · 6元/月"
                            1 -> "立即开通 · 28元/年"
                            else -> "立即开通 · 48元永久"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // 未登录提示
            if (!isLoggedIn) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "需要先登录账号才能开通哦~",
                    fontSize = 12.sp,
                    color = TextHint
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 权益说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "开通后享有以下权益",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PremiumBenefit("去除所有广告")
                    PremiumBenefit("数据云端同步")
                    PremiumBenefit("更多主题皮肤")
                    PremiumBenefit("详细统计报告")
                    PremiumBenefit("专属小兔徽章")
                    PremiumBenefit("优先体验新功能")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 温暖的话
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("致亲爱的你", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryPink)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "「来了么」是一个人的小小梦想，\n" +
                            "从深夜的代码到清晨的调试，\n" +
                            "每一个功能都倾注了用心和热爱。\n\n" +
                            "你的支持不仅仅是一杯咖啡的价格，\n" +
                            "更是对独立开发者最大的鼓励。\n" +
                            "谢谢你愿意为热爱买单。",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "开通即表示同意相关服务条款",
                fontSize = 10.sp,
                color = TextHint
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PricePlanCard(
    title: String,
    price: String,
    unit: String,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isSelected)
                    Modifier.border(2.dp, PrimaryPink, RoundedCornerShape(14.dp))
                else
                    Modifier.border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(14.dp))
            )
            .background(
                if (isSelected) PrimaryPink.copy(alpha = 0.08f) else Color.White,
                RoundedCornerShape(14.dp)
            )
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PrimaryPink)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) PrimaryPink else TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = price,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) PrimaryPink else TextPrimary
                )
                Text(
                    text = "元",
                    fontSize = 12.sp,
                    color = if (isSelected) PrimaryPink else TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                text = desc,
                fontSize = 10.sp,
                color = TextHint
            )
        }
    }
}

@Composable
private fun PremiumBenefit(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = AccentTeal
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextPrimary
        )
    }
}

// ──────────────── 头像上传工具函数 ────────────────

private suspend fun uploadAvatar(context: Context, token: String, uri: Uri): String? {
    return withContext(Dispatchers.IO) {
        try {
            val boundary = "----AvatarBoundary${System.currentTimeMillis()}"
            val url = URL("http://47.123.5.171:8080/api/user/avatar")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val imageBytes = inputStream.readBytes()
            inputStream.close()

            // 获取文件名和MIME类型
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val ext = when {
                mimeType.contains("png") -> ".png"
                mimeType.contains("webp") -> ".webp"
                else -> ".jpg"
            }
            val fileName = "avatar${ext}"

            conn.outputStream.use { os ->
                val writer = os.bufferedWriter()
                // 文件部分
                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"avatar\"; filename=\"$fileName\"\r\n")
                writer.write("Content-Type: $mimeType\r\n\r\n")
                writer.flush()
                os.write(imageBytes)
                os.flush()
                writer.write("\r\n--$boundary--\r\n")
                writer.flush()
            }

            val responseCode = conn.responseCode
            val response = if (responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }
            conn.disconnect()

            if (responseCode == 200) {
                val json = org.json.JSONObject(response)
                if (json.optInt("code", 0) == 200) {
                    json.optJSONObject("data")?.optString("avatarUrl")
                } else null
            } else null
        } catch (e: Exception) {
            android.util.Log.e("uploadAvatar", "上传失败", e)
            null
        }
    }
}

// ──────────────── 我的伴侣页面 ────────────────

@Composable
private fun PartnerContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user by AuthManager.userState.collectAsState()
    val token = remember {
        context.getSharedPreferences("laileme_auth", Context.MODE_PRIVATE)
            .getString("token", "") ?: ""
    }
    val isMale = user?.gender == "male"

    var partnerInfo by remember { mutableStateOf<PartnerInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var bindUid by remember { mutableStateOf("") }
    var isBinding by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var showUnbindDialog by remember { mutableStateOf(false) }

    // 伴侣模式开关（女方用）
    var partnerModeOn by remember { mutableStateOf(false) }
    // 待处理的绑定请求（女方用）
    var pendingRequests by remember { mutableStateOf<List<PartnerRequest>>(emptyList()) }

    // 加载伴侣信息和请求
    LaunchedEffect(Unit) {
        if (token.isNotEmpty()) {
            partnerInfo = PartnerManager.getPartnerInfo(token)
            partnerModeOn = partnerInfo?.partnerMode ?: false
            if (!isMale) {
                val (_, reqs) = PartnerManager.getPendingRequests(token)
                pendingRequests = reqs
            }
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryPink.copy(alpha = 0.1f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(20.dp),
                    tint = PrimaryPink
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "我的伴侣",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                CircularProgressIndicator(color = PrimaryPink, modifier = Modifier.padding(40.dp))
            } else if (user == null) {
                Icon(Icons.Outlined.FavoriteBorder, null, modifier = Modifier.size(64.dp), tint = TextHint)
                Spacer(modifier = Modifier.height(12.dp))
                Text("请先登录后再使用伴侣功能", color = TextSecondary, fontSize = 14.sp)
            } else if (partnerInfo?.bound == true) {
                // ═══ 已绑定 ═══
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    if (partnerInfo?.partnerGender == "male") Color(0xFF4FC3F7).copy(alpha = 0.15f)
                                    else PrimaryPink.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (partnerInfo?.partnerAvatarUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data("http://47.123.5.171:8080${partnerInfo!!.partnerAvatarUrl}")
                                        .crossfade(true).build(),
                                    contentDescription = "伴侣头像",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    if (partnerInfo?.partnerGender == "male") Icons.Outlined.Male else Icons.Outlined.Female,
                                    null, modifier = Modifier.size(36.dp),
                                    tint = if (partnerInfo?.partnerGender == "male") Color(0xFF4FC3F7) else PrimaryPink
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(partnerInfo?.partnerNickname ?: "", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("UID: ${partnerInfo?.partnerUid ?: ""}", fontSize = 12.sp, color = TextHint)
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = AccentTeal.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Favorite, null, modifier = Modifier.size(16.dp), tint = AccentTeal)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("已绑定", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AccentTeal)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = { showUnbindDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PeriodRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PeriodRed.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Outlined.HeartBroken, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("解除绑定", fontWeight = FontWeight.Medium)
                }
            } else {
                // ═══ 未绑定 ═══
                Icon(Icons.Outlined.FavoriteBorder, null, modifier = Modifier.size(64.dp), tint = PrimaryPink.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // 我的UID
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryPink.copy(alpha = 0.05f))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Badge, null, modifier = Modifier.size(20.dp), tint = PrimaryPink)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("我的UID（告诉Ta）", fontSize = 11.sp, color = TextHint)
                            Text(user?.uid ?: "--", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryPink, letterSpacing = 2.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (!isMale) {
                    // ── 女方：伴侣模式开关 ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Shield,
                                null, modifier = Modifier.size(24.dp),
                                tint = if (partnerModeOn) PrimaryPink else TextHint
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("伴侣模式", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(
                                    if (partnerModeOn) "已开启 - 对方可以向你发送绑定请求"
                                    else "关闭中 - 其他人无法向你发送绑定请求",
                                    fontSize = 11.sp, color = TextSecondary
                                )
                            }
                            Switch(
                                checked = partnerModeOn,
                                onCheckedChange = { wantOn ->
                                    scope.launch {
                                        val (ok, msg) = PartnerManager.setPartnerMode(token, wantOn)
                                        if (ok) {
                                            partnerModeOn = wantOn
                                            message = msg
                                            isError = false
                                        } else {
                                            message = msg
                                            isError = true
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = PrimaryPink,
                                    checkedThumbColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── 女方：待处理的绑定请求 ──
                    if (pendingRequests.isNotEmpty()) {
                        Text(
                            "收到的绑定请求",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        pendingRequests.forEach { req ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 请求方头像
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4FC3F7).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (req.fromAvatarUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data("http://47.123.5.171:8080${req.fromAvatarUrl}")
                                                    .crossfade(true).build(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Outlined.Person, null, modifier = Modifier.size(24.dp), tint = Color(0xFF4FC3F7))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(req.fromNickname.ifEmpty { "用户" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("UID: ${req.fromUid}", fontSize = 11.sp, color = TextHint)
                                    }
                                    // 同意按钮
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isLoading = true
                                                val (ok, msg) = PartnerManager.acceptRequest(token, req.fromId)
                                                if (ok) {
                                                    partnerInfo = PartnerManager.getPartnerInfo(token)
                                                    pendingRequests = emptyList()
                                                    message = "绑定成功"
                                                    isError = false
                                                } else {
                                                    message = msg
                                                    isError = true
                                                }
                                                isLoading = false
                                            }
                                        },
                                        modifier = Modifier.height(32.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text("同意", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // 拒绝按钮
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                val (ok, _) = PartnerManager.rejectRequest(token, req.fromId)
                                                if (ok) {
                                                    pendingRequests = pendingRequests.filter { it.fromId != req.fromId }
                                                    message = "已拒绝"
                                                    isError = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.height(32.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextHint),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text("拒绝", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else if (partnerModeOn) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Text(
                                "暂无绑定请求\n将你的UID告诉对方，等待Ta发送绑定请求",
                                modifier = Modifier.padding(20.dp),
                                fontSize = 13.sp,
                                color = TextHint,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // ── 男方：发送绑定请求 ──
                    Text(
                        "绑定伴侣",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "输入对方的UID发送绑定请求\n对方需要先开启伴侣模式并同意后才能绑定",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = bindUid,
                        onValueChange = { bindUid = it; message = null },
                        label = { Text("输入伴侣的UID") },
                        leadingIcon = {
                            Icon(Icons.Outlined.PersonSearch, null, modifier = Modifier.size(20.dp), tint = PrimaryPink.copy(alpha = 0.6f))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPink,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedLabelColor = PrimaryPink
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (bindUid.isBlank()) {
                                message = "请输入伴侣的UID"
                                isError = true
                                return@Button
                            }
                            scope.launch {
                                isBinding = true
                                message = null
                                val result = PartnerManager.sendBindRequest(token, bindUid.trim())
                                isBinding = false
                                message = result.second
                                isError = !result.first
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                        enabled = !isBinding && bindUid.isNotBlank()
                    ) {
                        if (isBinding) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("发送绑定请求", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 消息提示
                if (message != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isError) PeriodRed.copy(alpha = 0.1f) else AccentTeal.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(message!!, modifier = Modifier.padding(12.dp), fontSize = 13.sp, color = if (isError) PeriodRed else AccentTeal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // 解绑确认弹窗
    if (showUnbindDialog) {
        AlertDialog(
            onDismissRequest = { showUnbindDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(Icons.Outlined.HeartBroken, null, tint = PeriodRed, modifier = Modifier.size(32.dp))
            },
            title = { Text("确认解除绑定？", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("解绑后将无法查看对方的经期数据，双方都会取消绑定关系。", fontSize = 13.sp, color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        showUnbindDialog = false
                        isLoading = true
                        val result = PartnerManager.unbindPartner(token)
                        if (result.first) {
                            partnerInfo = PartnerInfo(bound = false)
                            // 清空男方本地的伴侣同步数据
                            if (isMale) {
                                try {
                                    val db = com.laileme.app.data.AppDatabase.getDatabase(context)
                                    val allRecords = db.periodDao().getAllList()
                                    if (allRecords.isNotEmpty()) {
                                        db.periodDao().deleteAll(allRecords)
                                    }
                                    db.sleepDao().deleteAll()
                                } catch (_: Exception) {}
                            }
                            message = "已解除绑定"
                            isError = false
                        } else {
                            message = result.second
                            isError = true
                        }
                        isLoading = false
                    }
                }) { Text("确认解绑", color = PeriodRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showUnbindDialog = false }) { Text("取消", color = TextHint) }
            }
        )
    }
}
