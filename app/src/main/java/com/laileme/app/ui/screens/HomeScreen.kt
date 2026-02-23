package com.laileme.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.animation.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laileme.app.R
import com.laileme.app.ui.PeriodUiState
import com.laileme.app.ui.normalizeDate
import com.laileme.app.ui.components.BunnyMascotLying
import androidx.compose.ui.graphics.vector.ImageVector
import com.laileme.app.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

private val periodTips = listOf(
    "经期前多补充铁质食物，如菠菜、红枣、黑芝麻哦~",
    "保持心情愉快，适当运动有助于缓解经期不适~",
    "经期注意保暖，避免着凉，热水袋是好帮手！",
    "多喝温水，少喝冷饮，对身体更好呢~",
    "规律作息有助于维持稳定的月经周期~",
    "经期前后适量补充维生素B6，可以缓解情绪波动~",
    "瑜伽和散步是经期友好的运动方式~",
    "经期饮食宜清淡，少吃辛辣刺激的食物~",
    "记录月经周期有助于了解自己的身体规律~",
    "排卵期前后注意观察身体变化，了解自己的生育窗口~",
    "经期可以适当吃些黑巧克力，有助于改善心情~",
    "充足的睡眠对月经规律非常重要~",
    "压力过大可能影响月经周期，记得放松自己~",
    "经期腹痛可以轻轻按摩腹部，顺时针方向哦~",
    "红糖姜茶是经期暖身的好选择~",
    "经期尽量避免剧烈运动和重体力劳动~",
    "多吃富含omega-3的食物有助于减轻痛经~",
    "经期前胸部胀痛是正常现象，不用太担心~",
    "保持个人卫生，勤换卫生用品很重要~",
    "如果经期异常，建议及时就医检查哦~",
    "香蕉富含钾元素，有助于缓解经期水肿~",
    "泡脚可以促进血液循环，缓解经期不适~",
    "经期适当补充钙质，有助于减少痉挛~",
    "每天保持30分钟的轻度运动对健康很有益~",
    "了解自己的PMS症状，提前做好应对准备~"
)

@Composable
fun HomeScreen(
    uiState: PeriodUiState,
    onAddPeriod: (Long) -> Unit,
    onEndPeriod: (Long) -> Unit,
    onReset: () -> Unit,
    onSaveSettings: (Int, Int) -> Unit,
    onSaveMode: (String) -> Unit
) {
    val hasRecord = uiState.latestRecord != null
    var showModeDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showManualSetupDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(16.dp)
            .padding(bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部栏：工具栏 + 小兔子
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeToolBar()
            BunnyMascotLying(modifier = Modifier.size(72.dp, 50.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 月亮背景 + 圆环
        Box(
            modifier = Modifier
                .size(300.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            // 月亮背景图
            Image(
                painter = painterResource(id = R.drawable.moon_bg),
                contentDescription = "月亮装饰",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            // 缩小的圆环，偏移对准月亮内圈（月牙左厚右薄，内圈偏右偏上）
            Box(
                modifier = Modifier
                    .size(115.dp)
                    .offset(x = 18.dp, y = (-41).dp),
                contentAlignment = Alignment.Center
            ) {
                CycleRing(uiState)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (hasRecord) {
                        Text("预计", fontSize = 8.sp, color = TextHint)
                        if (uiState.isInPeriod) {
                            Text(
                                "${uiState.daysUntilPeriodEnd}",
                                fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PeriodRed
                            )
                            Text("天后结束", fontSize = 10.sp, color = TextSecondary)
                        } else {
                            Text(
                                "${uiState.daysUntilNextPeriod}",
                                fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PrimaryPink
                            )
                            Text("天后来", fontSize = 10.sp, color = TextSecondary)
                        }
                        Text("周期第 ${uiState.cycleDay} 天", fontSize = 8.sp, color = TextHint)
                    } else {
                        Text("—", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextHint)
                        Text("记录经期", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 只统计已完成（有结束日期）的记录
        val completedRecords = uiState.records.filter { it.endDate != null }
        val latestCompleted = completedRecords.firstOrNull()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatusCard("周期", if (latestCompleted != null) "${latestCompleted.cycleLength}" else "--", "天", AccentTeal)
            StatusCard("经期", if (latestCompleted != null) "${latestCompleted.periodLength}" else "--", "天", PrimaryPink)
            StatusCard("记录", if (completedRecords.isNotEmpty()) "${completedRecords.size}" else "--", "次", AccentOrange)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 小贴士（随机轮播）
        var tipIndex by remember { mutableIntStateOf((Math.random() * periodTips.size).toInt()) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(6000)
                tipIndex = (tipIndex + (1..periodTips.size - 1).random()) % periodTips.size
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = LightPink),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.LightbulbCircle,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = PrimaryPink
                )
                Spacer(modifier = Modifier.width(4.dp))
                AnimatedContent(
                    targetState = tipIndex,
                    transitionSpec = {
                        (fadeIn() + slideInVertically { it }) togetherWith
                                (fadeOut() + slideOutVertically { -it })
                    },
                    label = "tip"
                ) { index ->
                    Text(
                        text = periodTips[index],
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 操作按钮
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HomeIconActionButton(
                    icon = Icons.Outlined.PlayCircleOutline,
                    label = "月经开始",
                    color = if (!uiState.hasActivePeriod) PrimaryPink else TextHint,
                    onClick = {
                        if (!uiState.hasActivePeriod) {
                            if (!uiState.hasSetup) {
                                // 首次使用，弹出模式选择对话框
                                showModeDialog = true
                            } else {
                                // 已设置过，直接记录
                                onAddPeriod(normalizeDate(System.currentTimeMillis()))
                            }
                        }
                    }
                )
                HomeIconActionButton(
                    icon = Icons.Outlined.StopCircle,
                    label = "月经结束",
                    color = if (uiState.hasActivePeriod) PeriodRed else TextHint,
                    onClick = {
                        if (uiState.hasActivePeriod) onEndPeriod(normalizeDate(System.currentTimeMillis()))
                    }
                )
                HomeIconActionButton(
                    icon = Icons.Outlined.Refresh,
                    label = "重置",
                    color = if (uiState.latestRecord != null) AccentOrange else TextHint,
                    onClick = {
                        if (uiState.latestRecord != null) showResetConfirm = true
                    }
                )
            }
        }
    }

    // 首次使用 - 模式选择对话框
    if (showModeDialog) {
        ModeSelectionDialog(
            onDismiss = { showModeDialog = false },
            onSelectAuto = {
                // 选择自动推算模式：保存模式，直接开始记录
                onSaveMode("auto")
                onAddPeriod(normalizeDate(System.currentTimeMillis()))
                showModeDialog = false
            },
            onSelectManual = {
                // 选择手动输入模式：先保存模式，再弹出周期设置
                onSaveMode("manual")
                showModeDialog = false
                showManualSetupDialog = true
            }
        )
    }

    // 手动模式 - 输入周期设置对话框
    if (showManualSetupDialog) {
        ManualSetupDialog(
            onDismiss = { showManualSetupDialog = false },
            onConfirm = { cycle, period ->
                onSaveSettings(cycle, period)
                onAddPeriod(normalizeDate(System.currentTimeMillis()))
                showManualSetupDialog = false
            }
        )
    }

    // 重置确认弹窗
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    "确认重置",
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange
                )
            },
            text = {
                Text(
                    "将删除当前周期记录，历史已完成的记录不受影响。确定要重置吗？",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReset()
                        showResetConfirm = false
                    }
                ) {
                    Text("确认重置", color = AccentOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("取消", color = TextHint)
                }
            }
        )
    }
}

@Composable
private fun HomeIconActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun ModeSelectionDialog(
    onDismiss: () -> Unit,
    onSelectAuto: () -> Unit,
    onSelectManual: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                "选择记录模式",
                color = PrimaryPink, fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
        },
        text = {
            Column {
                Text(
                    "欢迎使用来了么~ 请选择适合你的记录方式：",
                    color = TextSecondary, fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 自动推算模式
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelectAuto),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryPink.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = PrimaryPink
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "先记录，自动推算",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPink
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "适合不清楚自己周期的用户。每次记录经期的开始和结束，系统会根据你的历史数据自动计算周期长度和经期天数，越用越准哦~",
                            fontSize = 11.sp, color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "使用步骤：点击开始 → 经期结束时点结束 → 下次点开始时系统就能算出你的周期啦",
                            fontSize = 10.sp, color = PrimaryPink.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 手动输入模式
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelectManual),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentTeal.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.EditCalendar,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = AccentTeal
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "手动输入周期",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentTeal
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "适合已经了解自己周期的用户。直接输入你的月经周期天数和经期天数，系统将使用固定值来预测。",
                            fontSize = 11.sp, color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 温馨提示卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = AccentOrange
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "温馨提示",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "• 选择「自动推算」后，第一次记录时首页圆环显示为 0 是正常的哦，因为还没有历史数据来计算周期~\n• 完成第一次完整记录（开始→结束）后，第二次点击开始时系统就会自动推算出周期啦！\n• 后续可在「我的」页面随时修改记录模式和周期设置~",
                            fontSize = 10.sp, color = TextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("暂不设置", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun ManualSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var cycleLength by remember { mutableStateOf("28") }
    var periodLength by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                "设置周期参数",
                color = PrimaryPink, fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
        },
        text = {
            Column {
                Text(
                    "请输入你的月经周期信息：",
                    color = TextSecondary, fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = cycleLength,
                    onValueChange = { cycleLength = it.filter { c -> c.isDigit() } },
                    label = { Text("月经周期（天）", fontSize = 12.sp) },
                    placeholder = { Text("通常21-35天", fontSize = 11.sp, color = TextHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPink,
                        focusedLabelColor = PrimaryPink
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = periodLength,
                    onValueChange = { periodLength = it.filter { c -> c.isDigit() } },
                    label = { Text("经期天数（天）", fontSize = 12.sp) },
                    placeholder = { Text("通常3-7天", fontSize = 11.sp, color = TextHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPink,
                        focusedLabelColor = PrimaryPink
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cycle = (cycleLength.toIntOrNull() ?: 28).coerceIn(15, 60)
                val period = (periodLength.toIntOrNull() ?: 5).coerceIn(1, 15)
                onConfirm(cycle, period)
            }) {
                Text("确定并记录", color = PrimaryPink, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun CycleRing(uiState: PeriodUiState) {
    val cycleLength = uiState.latestRecord?.cycleLength ?: 28
    val periodLength = uiState.latestRecord?.periodLength ?: 5
    val cycleDay = uiState.cycleDay
    val isInPeriod = uiState.isInPeriod
    val hasRecord = uiState.latestRecord != null

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 10.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        // ── 主圆环背景 ──
        drawCircle(
            color = Color(0xFFF0F0F0).copy(alpha = 0.6f),
            radius = radius, center = center,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // ── 进度弧 ──
        // 经期中：按经期天数算进度（如4天经期过了1天=25%）
        // 非经期：按"经期结束→下次经期开始"的间隔算进度，越满越接近下次经期
        if (hasRecord && cycleDay > 0) {
            val progressSweep = if (isInPeriod) {
                val periodDay = cycleDay.coerceAtMost(periodLength)
                ((periodDay - 1).toFloat() / periodLength) * 360f
            } else {
                val nonPeriodDays = (cycleLength - periodLength).coerceAtLeast(1)
                val daysSincePeriodEnd = (cycleDay - periodLength).coerceAtLeast(0)
                (daysSincePeriodEnd.toFloat() / nonPeriodDays) * 360f
            }
            val ringColor = if (isInPeriod) PeriodRed else PrimaryPink
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = progressSweep.coerceAtLeast(1f),
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            val angle = (progressSweep - 90f) * (Math.PI / 180.0)
            val ix = center.x + radius * cos(angle).toFloat()
            val iy = center.y + radius * sin(angle).toFloat()
            drawCircle(Color.White, 8.dp.toPx(), Offset(ix, iy))
            drawCircle(ringColor, 5.dp.toPx(), Offset(ix, iy))
        }
    }
}

@Composable
private fun HomeToolBar() {
    Surface(
        modifier = Modifier.padding(top = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeToolBarIcon(Icons.Outlined.Menu, false)
            HomeToolBarIcon(Icons.Outlined.CalendarMonth, false)
            HomeToolBarIcon(Icons.Outlined.DarkMode, false)
            HomeToolBarIcon(Icons.Outlined.ChatBubbleOutline, false)
        }
    }
}

@Composable
private fun HomeToolBarIcon(icon: ImageVector, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AccentOrange.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isSelected) AccentOrange else TextSecondary
        )
    }
}

@Composable
private fun StatusCard(title: String, value: String, unit: String, color: Color) {
    Card(
        modifier = Modifier.width(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 10.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
                Text(unit, fontSize = 9.sp, color = TextHint, modifier = Modifier.padding(bottom = 1.dp))
            }
        }
    }
}
