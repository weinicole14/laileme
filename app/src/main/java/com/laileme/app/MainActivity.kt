package com.laileme.app

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laileme.app.notification.NotificationHelper
import com.laileme.app.notification.NotificationScheduler
import com.laileme.app.data.AuthManager
import com.laileme.app.ui.PeriodViewModel
import com.laileme.app.ui.components.BottomNavBar
import com.laileme.app.ui.components.NavItem
import com.laileme.app.ui.screens.*
import com.laileme.app.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化通知渠道 & 定时任务
        NotificationHelper.createChannels(this)
        NotificationScheduler.scheduleAll(this)

        // 适配高帧率（120Hz / 90Hz）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用 Display.Mode API
            window.attributes = window.attributes.also { params ->
                val display = display ?: windowManager.defaultDisplay
                val supportedModes = display.supportedModes
                val bestMode = supportedModes.maxByOrNull { it.refreshRate }
                if (bestMode != null) {
                    params.preferredDisplayModeId = bestMode.modeId
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6~10 使用 preferredRefreshRate
            window.attributes = window.attributes.also { params ->
                @Suppress("DEPRECATION")
                val display = windowManager.defaultDisplay
                val supportedModes = display.supportedModes
                val maxRate = supportedModes.maxOfOrNull { it.refreshRate } ?: 60f
                params.preferredRefreshRate = maxRate
            }
        }

        // 初始化主题管理器
        com.laileme.app.ui.theme.ThemeManager.init(this)

        // 初始化认证管理器（接入阿里云服务器）
        com.laileme.app.data.AuthManager.init(this)
        com.laileme.app.data.AuthManager.setAuthService(
            com.laileme.app.data.RemoteAuthService("http://47.123.5.171:8080/")
        )

        setContent {
            LailemeTheme {
                // ── 开屏界面状态 ──
                var showSplash by remember { mutableStateOf(true) }
                var splashVisible by remember { mutableStateOf(true) }

                // ── 密码锁状态 ──
                val appPrefs = remember { getSharedPreferences("laileme_settings", Context.MODE_PRIVATE) }
                val savedPassword = appPrefs.getString("app_password", "") ?: ""
                val hasPassword = savedPassword.isNotEmpty()
                var isLocked by remember { mutableStateOf(hasPassword) }

                LaunchedEffect(Unit) {
                    delay(1500) // 显示1.5秒
                    splashVisible = false // 开始淡出
                    delay(500) // 淡出动画时长
                    showSplash = false // 完全移除
                }

                val viewModel: PeriodViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()
                val currentDiary by viewModel.currentDiary.collectAsState()
                // 根据性别决定可见的导航项（男性隐藏"记录"和"发现"）
                val userGender = AuthManager.userState.collectAsState().value?.gender ?: "female"
                val visibleNavItems = remember(userGender) {
                    if (userGender == "male") {
                        listOf(NavItem.HOME, NavItem.CALENDAR, NavItem.STATS, NavItem.PROFILE)
                    } else {
                        NavItem.entries.toList()
                    }
                }
                var selectedNav by remember { mutableStateOf(NavItem.HOME) }
                // 记录上一个页面索引，用于判断滑动方向
                var previousNavIndex by remember { mutableIntStateOf(0) }
                // 是否在子页面（设置/个人档案），用于隐藏底部导航栏
                var isInSubPage by remember { mutableStateOf(false) }

                // 双击退出：一级页面且不在子页面时拦截返回键
                var lastBackTime by remember { mutableLongStateOf(0L) }
                BackHandler(enabled = !isInSubPage) {
                    val now = System.currentTimeMillis()
                    if (now - lastBackTime < 2000) {
                        finish()
                    } else {
                        lastBackTime = now
                        Toast.makeText(this@MainActivity, "再按一次退出来了么", Toast.LENGTH_SHORT).show()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background)
                ) {
                    // 带平移动画的页面切换
                    AnimatedContent(
                        targetState = selectedNav,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            val currentIndex = visibleNavItems.indexOf(targetState).coerceAtLeast(0)
                            val prevIndex = previousNavIndex

                            (if (currentIndex > prevIndex) {
                                (slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth / 4 },
                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(200))) togetherWith
                                (slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> -fullWidth / 4 },
                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(120)))
                            } else {
                                (slideInHorizontally(
                                    initialOffsetX = { fullWidth -> -fullWidth / 4 },
                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(200))) togetherWith
                                (slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> fullWidth / 4 },
                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(120)))
                            }).using(SizeTransform(clip = true) { _, _ -> snap() })
                        },
                        label = "page_transition"
                    ) { nav ->
                        when (nav) {
                            NavItem.HOME -> HomeScreen(
                                uiState = uiState,
                                onAddPeriod = { date -> viewModel.addPeriodRecord(date) },
                                onEndPeriod = { date -> viewModel.endPeriod(date) },
                                onReset = { viewModel.resetLatestRecord() },
                                onSaveSettings = { cycle, period -> viewModel.saveCycleSettings(cycle, period) },
                                onSaveMode = { mode -> viewModel.saveTrackingMode(mode) }
                            )
                            NavItem.CALENDAR -> CalendarScreen(
                                uiState = uiState,
                                diaryEntry = currentDiary,
                                onAddPeriod = { date -> viewModel.addPeriodRecord(date) },
                                onEndPeriod = { date -> viewModel.endPeriod(date) },
                                onDateSelected = { viewModel.updateSelectedDate(it) },
                                onMonthChange = { viewModel.changeMonth(it) }
                            )
                            NavItem.DIARY -> DiaryScreen(
                                uiState = uiState,
                                diaryEntry = currentDiary,
                                onDateSelected = { viewModel.updateSelectedDate(it) },
                                onSaveDiary = { entry -> viewModel.saveDiary(entry) }
                            )
                            NavItem.STATS -> StatsScreen(records = uiState.records)
                            NavItem.DISCOVER -> DiscoverScreen()
                            NavItem.PROFILE -> ProfileScreen(
                                uiState = uiState,
                                onSaveSettings = { cycle, period -> viewModel.saveCycleSettings(cycle, period) },
                                onSaveMode = { mode -> viewModel.saveTrackingMode(mode) },
                                onSubPageChanged = { inSubPage -> isInSubPage = inSubPage }
                            )
                        }
                    }

                    // 底部导航栏（子页面时隐藏）
                    AnimatedVisibility(
                        visible = !isInSubPage,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(250)
                        ) + fadeIn(tween(250)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(250)
                        ) + fadeOut(tween(150)),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                    BottomNavBar(
                            selectedItem = selectedNav,
                            onItemSelected = { newNav ->
                                previousNavIndex = visibleNavItems.indexOf(selectedNav).coerceAtLeast(0)
                                selectedNav = newNav
                            },
                            visibleItems = visibleNavItems
                        )
                    }

                    // ── 开屏界面覆盖层 ──
                    if (showSplash) {
                        val splashAlpha by animateFloatAsState(
                            targetValue = if (splashVisible) 1f else 0f,
                            animationSpec = tween(500),
                            label = "splash_fade"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(splashAlpha)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.splash_bunny),
                                contentDescription = "开屏",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // ── 密码锁界面覆盖层 ──
                    if (hasPassword && isLocked && !showSplash) {
                        LockScreenOverlay(
                            savedPassword = savedPassword,
                            onUnlock = { isLocked = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LockScreenOverlay(
    savedPassword: String,
    onUnlock: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        if (error) {
            kotlinx.coroutines.delay(1200)
            error = false
        }
    }

    LaunchedEffect(pin) {
        if (pin.length == 4) {
            if (pin == savedPassword) {
                onUnlock()
            } else {
                error = true
                pin = ""
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFF0F3),
                        Color(0xFFFFF8FA),
                        Color(0xFFFFF0F3)
                    )
                )
            )
            .pointerInput(Unit) {
                // 消费所有触摸事件，阻止穿透到下层
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                    }
                }
            }
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 锁图标
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PrimaryPink.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = PrimaryPink
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "来了么",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPink
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "请输入密码解锁",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PIN 点位
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < pin.length) PrimaryPink
                                else Color(0xFFE0E0E0)
                            )
                    )
                }
            }

            // 错误提示
            Spacer(modifier = Modifier.height(12.dp))
            if (error) {
                Text(
                    "密码错误，请重试",
                    fontSize = 12.sp,
                    color = Color(0xFFE53935),
                    fontWeight = FontWeight.Medium
                )
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 数字键盘
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫")
            )
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    row.forEach { key ->
                        when (key) {
                            "" -> Spacer(modifier = Modifier.size(64.dp))
                            "⌫" -> Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF0F0F0))
                                    .clickable {
                                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Backspace,
                                    contentDescription = "删除",
                                    modifier = Modifier.size(22.dp),
                                    tint = TextSecondary
                                )
                            }
                            else -> Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable {
                                        if (pin.length < 4) pin += key
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    key,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
