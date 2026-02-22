package com.laileme.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laileme.app.ui.PeriodViewModel
import com.laileme.app.ui.components.BottomNavBar
import com.laileme.app.ui.components.NavItem
import com.laileme.app.ui.screens.*
import com.laileme.app.ui.theme.Background
import com.laileme.app.ui.theme.LailemeTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LailemeTheme {
                // ── 开屏界面状态 ──
                var showSplash by remember { mutableStateOf(true) }
                var splashVisible by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1500) // 显示1.5秒
                    splashVisible = false // 开始淡出
                    delay(500) // 淡出动画时长
                    showSplash = false // 完全移除
                }

                val viewModel: PeriodViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()
                val currentDiary by viewModel.currentDiary.collectAsState()
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
                        transitionSpec = {
                            val currentIndex = targetState.ordinal
                            val prevIndex = previousNavIndex

                            if (currentIndex > prevIndex) {
                                // 向右切换：新页面从右边滑入，旧页面向左滑出
                                (slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(300)
                                ) + fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> -fullWidth / 3 },
                                    animationSpec = tween(300)
                                ) + fadeOut(animationSpec = tween(150)))
                            } else {
                                // 向左切换：新页面从左边滑入，旧页面向右滑出
                                (slideInHorizontally(
                                    initialOffsetX = { fullWidth -> -fullWidth },
                                    animationSpec = tween(300)
                                ) + fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> fullWidth / 3 },
                                    animationSpec = tween(300)
                                ) + fadeOut(animationSpec = tween(150)))
                            }.using(SizeTransform(clip = false))
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
                                onMonthChange = { viewModel.changeMonth(it) },
                                onSaveDiary = { date, mood, symptoms, notes ->
                                    viewModel.saveDiary(date, mood, symptoms, notes)
                                }
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
                                previousNavIndex = selectedNav.ordinal
                                selectedNav = newNav
                            }
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
                }
            }
        }
    }
}
