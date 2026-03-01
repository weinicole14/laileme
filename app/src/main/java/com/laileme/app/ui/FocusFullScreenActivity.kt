package com.laileme.app.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laileme.app.ui.theme.*

class FocusFullScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        FocusTimerState.init(this)

        setContent {
            MaterialTheme {
                FocusFullScreenContent(onExit = { finish() })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
private fun FocusFullScreenContent(onExit: () -> Unit) {
    val isRunning by FocusTimerState.isRunning.collectAsState()
    val isPaused by FocusTimerState.isPaused.collectAsState()
    val isBreak by FocusTimerState.isBreak.collectAsState()
    val remainingSeconds by FocusTimerState.remainingSeconds.collectAsState()
    val completedToday by FocusTimerState.completedToday.collectAsState()
    val focusDuration by FocusTimerState.focusDuration.collectAsState()
    val breakDuration by FocusTimerState.breakDuration.collectAsState()

    val totalSeconds = if (isBreak) breakDuration * 60 else focusDuration * 60
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 1f

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val activeColor = if (isBreak) Color(0xFF66BB6A) else Color(0xFFFF7043)
    val bgColor = if (isBreak) Color(0xFF1B3A1B) else Color(0xFF2D1B0E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // 右上角退出按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable { onExit() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Close, "退出", Modifier.size(22.dp), tint = Color.White.copy(alpha = 0.7f))
        }

        // 左上角番茄计数
        if (completedToday > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayCount = completedToday.coerceAtMost(8)
                repeat(displayCount) {
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(activeColor.copy(alpha = 0.6f))
                    )
                }
                if (completedToday > 8) {
                    Text(
                        "+${completedToday - 8}",
                        fontSize = 12.sp,
                        color = activeColor.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 主内容
        val timerSize = if (isLandscape) 260.dp else 300.dp
        val timeFontSize = if (isLandscape) 72.sp else 80.sp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 状态标签
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = activeColor.copy(alpha = 0.2f)
            ) {
                Text(
                    if (isBreak) "休息中" else "专注中",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeColor
                )
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 30.dp))

            // 大圆环计时器
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(300),
                label = "fullProgress"
            )

            Box(
                modifier = Modifier.size(timerSize),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 16f
                    val arcSize = size.width - strokeW * 2
                    val topLeft = androidx.compose.ui.geometry.Offset(strokeW, strokeW)

                    drawArc(
                        color = activeColor.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeW,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                    drawArc(
                        color = activeColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeW,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }

                // 大号时间
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val min = remainingSeconds / 60
                    val sec = remainingSeconds % 60
                    Text(
                        String.format("%02d:%02d", min, sec),
                        fontSize = timeFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "第 ${completedToday + 1} 个番茄",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 30.dp))

            // 控制按钮
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRunning) {
                    // 放弃
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { FocusTimerState.stop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Stop, "放弃", Modifier.size(24.dp), tint = Color(0xFFEF5350))
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // 暂停/继续
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = activeColor,
                        shadowElevation = 8.dp,
                        modifier = Modifier.clickable { FocusTimerState.togglePause() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 36.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                                null, Modifier.size(24.dp), tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isPaused) "继续" else "暂停",
                                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White
                            )
                        }
                    }
                } else {
                    // 开始
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = activeColor,
                        shadowElevation = 8.dp,
                        modifier = Modifier.clickable { FocusTimerState.start() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 40.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.PlayArrow, null, Modifier.size(26.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("开始专注", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 10.dp else 20.dp))

            // 底部提示
            Text(
                if (isRunning && !isPaused) "放下手机，专注当下" else "点击开始你的番茄时间",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.35f)
            )
        }
    }
}
