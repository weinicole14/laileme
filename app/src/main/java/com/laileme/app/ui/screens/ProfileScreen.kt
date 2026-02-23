package com.laileme.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import com.laileme.app.data.AuthManager
import com.laileme.app.data.PaymentManager
import com.laileme.app.data.SyncManager
import com.laileme.app.data.UserInfo
import com.laileme.app.ui.PeriodUiState
import com.laileme.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

// 页面状态枚举
private enum class ProfilePage { MAIN, SETTINGS, PROFILE_INFO, LOGIN, ABOUT, PREMIUM }

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
        currentPage = ProfilePage.MAIN
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
                    }
                )
                ProfilePage.SETTINGS -> SettingsContent(
                    uiState = uiState,
                    onSaveSettings = onSaveSettings,
                    onSaveMode = onSaveMode,
                    onBack = {
                        previousPage = ProfilePage.SETTINGS
                        currentPage = ProfilePage.MAIN
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
    onOpenPremium: () -> Unit = {}
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(12.dp)
            .padding(bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // 头像 — 点击可登录/查看
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    if (isLoggedIn) PrimaryPink.copy(alpha = 0.2f)
                    else Color(0xFFE0E0E0).copy(alpha = 0.4f)
                )
                .clickable {
                    if (!isLoggedIn) onOpenLogin()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isLoggedIn) Icons.Outlined.FavoriteBorder else Icons.Outlined.PersonOutline,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = if (isLoggedIn) PrimaryPink else TextHint
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoggedIn) {
            // 已登录：显示昵称
            Text(
                text = displayName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
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
                .clickable(onClick = onOpenPremium),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF0F5)
            ),
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
                        text = "小兔礼盒",
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

        // 个人档案入口
        ProfileMenuCard(
            icon = Icons.Outlined.Person,
            title = "个人档案",
            subtitle = "生日、身高、体重、血型",
            onClick = onOpenProfile
        )

        // 设置入口
        ProfileMenuCard(
            icon = Icons.Outlined.Settings,
            title = "设置",
            subtitle = "经期设置、通知、外观",
            onClick = onOpenSettings
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
            Spacer(modifier = Modifier.height(8.dp))
            ProfileMenuCard(
                icon = Icons.Outlined.Logout,
                title = "退出登录",
                subtitle = "当前账号：${user?.userId ?: ""}",
                onClick = {
                    scope.launch { AuthManager.logout() }
                }
            )
        }
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
                    text = "版本 beta 1.0",
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
                    "邮箱：support@weinicole.cn\n" +
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

// ──────────────── 小兔礼盒 付费页面 ────────────────

@Composable
private fun PremiumContent(onBack: () -> Unit, onOpenLogin: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPlan by remember { mutableIntStateOf(1) } // 0=月付, 1=年付, 2=永久
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
            text = { Text("感谢你的支持～小兔礼盒已为你开启全部权益 ♡", color = TextSecondary) },
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
                text = "小兔礼盒",
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
                text = "小兔礼盒",
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
                    price = "18",
                    unit = "元/年",
                    desc = "超值推荐",
                    isSelected = selectedPlan == 1,
                    onClick = { selectedPlan = 1 },
                    modifier = Modifier.weight(1f),
                    badge = "省50%"
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
                            1 -> "立即开通 · 18元/年"
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
                "开通即表示同意相关服务条款 · 支持7天无理由退款",
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
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryPink.copy(alpha = 0.08f) else Color.White
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, PrimaryPink)
        else
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8E8E8)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
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
