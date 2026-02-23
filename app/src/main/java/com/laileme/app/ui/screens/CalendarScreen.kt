package com.laileme.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laileme.app.data.entity.DiaryEntry
import com.laileme.app.ui.PeriodUiState
import com.laileme.app.ui.normalizeDate
import com.laileme.app.ui.components.BunnyMascot
import com.laileme.app.ui.components.CalendarView
import com.laileme.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// 心情数据
private data class MoodOption(val key: String, val icon: ImageVector, val label: String)
private val moodOptions = listOf(
    MoodOption("happy", Icons.Outlined.SentimentSatisfied, "开心"),
    MoodOption("sad", Icons.Outlined.SentimentDissatisfied, "难过"),
    MoodOption("angry", Icons.Outlined.SentimentVeryDissatisfied, "烦躁"),
    MoodOption("sleepy", Icons.Outlined.Bedtime, "困倦"),
    MoodOption("love", Icons.Outlined.FavoriteBorder, "幸福"),
    MoodOption("pain", Icons.Outlined.SentimentNeutral, "不适"),
    MoodOption("sick", Icons.Outlined.LocalHospital, "生病"),
    MoodOption("strong", Icons.Outlined.FitnessCenter, "元气")
)

@Composable
fun CalendarScreen(
    uiState: PeriodUiState,
    diaryEntry: DiaryEntry?,
    onAddPeriod: (Long) -> Unit,
    onEndPeriod: (Long) -> Unit,
    onDateSelected: (Long) -> Unit,
    onMonthChange: (Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(bottom = 60.dp)
    ) {
        TopSection(uiState)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            CalendarCard(uiState, onDateSelected, onMonthChange)
            LegendSection()
            // 预测免责提示
            Text(
                "※ 预测结果仅供参考，月经周期可能因生活习惯、压力、健康状况等因素而变化",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                fontSize = 9.sp,
                color = TextHint,
                textAlign = TextAlign.Center
            )
            DiaryViewSection(
                selectedDate = uiState.selectedDate,
                diaryEntry = diaryEntry
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddDialog) {
        AddPeriodDialog(
            selectedDate = uiState.selectedDate,
            onDismiss = { showAddDialog = false },
            onConfirm = { date ->
                onAddPeriod(date)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun TopSection(uiState: PeriodUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.White, Background)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BunnyMascot(modifier = Modifier.size(56.dp, 68.dp))
                CountdownBubble(uiState)
            }
        }
    }
}

@Composable
private fun CountdownBubble(uiState: PeriodUiState) {
    val hasRecord = uiState.latestRecord != null
    val text = when {
        !hasRecord -> "记录经期"
        uiState.isInPeriod -> "还有 ${uiState.daysUntilPeriodEnd} 天结束"
        else -> "还有 ${uiState.daysUntilNextPeriod} 天来"
    }
    val bgColor = when {
        !hasRecord -> AccentBlue.copy(alpha = 0.3f)
        uiState.isInPeriod -> PeriodRed.copy(alpha = 0.3f)
        else -> AccentBlue.copy(alpha = 0.3f)
    }
    Surface(
        modifier = Modifier.offset(y = (-4).dp),
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Text(
            text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary
        )
    }
}

@Composable
private fun CalendarCard(
    uiState: PeriodUiState,
    onDateSelected: (Long) -> Unit,
    onMonthChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        CalendarView(
            currentMonth = uiState.currentMonth,
            records = uiState.records,
            selectedDate = uiState.selectedDate,
            onDateSelected = onDateSelected,
            onMonthChange = onMonthChange
        )
    }
}

@Composable
private fun LegendSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(PeriodRed, "经期")
        LegendItem(PredictPeriod, "预测经期")
        LegendItem(FertileGreen, "易孕期")
        LegendItem(OvulationOrange, "排卵日")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(7.dp).clip(CircleShape).background(color)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(label, fontSize = 9.sp, color = TextSecondary)
    }
}

@Composable
private fun BottomActionPanel(
    selectedDate: Long,
    hasActivePeriod: Boolean,
    onStartPeriod: () -> Unit,
    onEndPeriod: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = BottomSheetBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(28.dp).height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(TextHint)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dateFormat.format(Date(selectedDate)),
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary
                )
                Text("⋮", fontSize = 14.sp, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    icon = Icons.Outlined.PlayCircleOutline,
                    label = "月经开始",
                    color = if (!hasActivePeriod) PrimaryPink else TextHint,
                    onClick = if (!hasActivePeriod) onStartPeriod else { {} }
                )
                ActionButton(
                    icon = Icons.Outlined.StopCircle,
                    label = "月经结束",
                    color = if (hasActivePeriod) PeriodRed else TextHint,
                    onClick = if (hasActivePeriod) onEndPeriod else { {} }
                )
                ActionButton(Icons.Outlined.AddCircleOutline, "状态", AccentTeal) { }
            }
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 9.sp, color = TextSecondary)
    }
}

@Composable
private fun DiaryViewSection(
    selectedDate: Long,
    diaryEntry: DiaryEntry?
) {
    val dateFormat = SimpleDateFormat("M月d日", Locale.CHINESE)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.EventNote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = PrimaryPink
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${dateFormat.format(Date(selectedDate))} 的记录",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (diaryEntry == null) {
                // 无记录提示
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFA), RoundedCornerShape(10.dp))
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.StickyNote2,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = TextHint
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "该日期暂无记录",
                            fontSize = 12.sp,
                            color = TextHint
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "可前往「记录」页面添加~",
                            fontSize = 10.sp,
                            color = TextHint
                        )
                    }
                }
            } else {
                // ── 心情展示 ──
                if (diaryEntry.mood.isNotEmpty()) {
                    val moodMatch = moodOptions.find { it.key == diaryEntry.mood }
                    if (moodMatch != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PrimaryPink.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = moodMatch.icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = PrimaryPink
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "心情：${moodMatch.label}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryPink
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // ── 身体状态摘要 ──
                val statusItems = buildList {
                    if (diaryEntry.flowLevel > 0) {
                        val label = when (diaryEntry.flowLevel) { 1 -> "少"; 2 -> "中"; 3 -> "多"; else -> "" }
                        add("月经量" to label)
                    }
                    if (diaryEntry.flowColor.isNotEmpty()) {
                        val label = when (diaryEntry.flowColor) {
                            "light_red" -> "浅红"; "red" -> "正红"; "dark_red" -> "深红"
                            "brown" -> "棕色"; "black" -> "黑色"; else -> diaryEntry.flowColor
                        }
                        add("经血颜色" to label)
                    }
                    if (diaryEntry.painLevel > 0) {
                        val label = when (diaryEntry.painLevel) { 1 -> "轻微"; 2 -> "中等"; 3 -> "较重"; 4 -> "严重"; else -> "" }
                        add("痛经" to label)
                    }
                    if (diaryEntry.breastPain > 0) {
                        val label = when (diaryEntry.breastPain) { 1 -> "轻微"; 2 -> "明显"; 3 -> "严重"; else -> "" }
                        add("胸部胀痛" to label)
                    }
                    if (diaryEntry.backPain > 0) {
                        val label = when (diaryEntry.backPain) { 1 -> "轻微"; 2 -> "明显"; 3 -> "严重"; else -> "" }
                        add("腰腹痛" to label)
                    }
                    if (diaryEntry.headache > 0) {
                        val label = when (diaryEntry.headache) { 1 -> "轻微"; 2 -> "明显"; 3 -> "严重"; else -> "" }
                        add("头痛" to label)
                    }
                    if (diaryEntry.digestive > 0) {
                        val label = when (diaryEntry.digestive) { 1 -> "不适"; 2 -> "腹泻"; 3 -> "便秘"; else -> "" }
                        add("肠胃" to label)
                    }
                    if (diaryEntry.fatigue > 0) {
                        val label = when (diaryEntry.fatigue) { 1 -> "正常"; 2 -> "有点累"; 3 -> "很疲惫"; else -> "" }
                        add("疲劳" to label)
                    }
                    if (diaryEntry.skinCondition.isNotEmpty()) {
                        val label = when (diaryEntry.skinCondition) {
                            "good" -> "很好"; "normal" -> "正常"; "oily" -> "出油"
                            "acne" -> "长痘"; "dry" -> "干燥"; else -> diaryEntry.skinCondition
                        }
                        add("皮肤" to label)
                    }
                    if (diaryEntry.temperature.isNotEmpty()) {
                        add("体温" to "${diaryEntry.temperature}°C")
                    }
                    if (diaryEntry.appetite > 0) {
                        val label = when (diaryEntry.appetite) { 1 -> "增加"; 2 -> "减少"; else -> "" }
                        add("食欲" to label)
                    }
                    if (diaryEntry.discharge.isNotEmpty()) {
                        val label = when (diaryEntry.discharge) {
                            "none" -> "无"; "clear" -> "透明"; "white" -> "白色"
                            "yellow" -> "黄色"; "sticky" -> "粘稠"; else -> diaryEntry.discharge
                        }
                        add("分泌物" to label)
                    }
                }

                if (statusItems.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.MonitorHeart,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = AccentTeal
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("身体状态", fontSize = 11.sp, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // 用 FlowRow 风格展示标签
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        statusItems.forEach { (name, value) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AccentTeal.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    "$name: $value",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    color = AccentTeal,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ── 日记内容 ──
                if (diaryEntry.notes.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = PrimaryPink
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("日记", fontSize = 11.sp, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFFAFB)
                    ) {
                        Text(
                            diaryEntry.notes,
                            modifier = Modifier.padding(10.dp),
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // 如果什么都没有记录（不太可能但防御性处理）
                if (diaryEntry.mood.isEmpty() && statusItems.isEmpty() && diaryEntry.notes.isEmpty()) {
                    Text(
                        "已有记录但内容为空",
                        fontSize = 11.sp,
                        color = TextHint,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPeriodDialog(
    selectedDate: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy年M月d日", Locale.CHINESE)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("记录经期开始", color = PrimaryPink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column {
                Text(
                    "选择的日期: ${dateFormat.format(Date(selectedDate))}",
                    color = TextSecondary, fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "将使用你在设置中配置的周期和经期天数",
                    color = TextHint, fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(selectedDate)
            }) {
                Text("确定", color = PrimaryPink, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}
