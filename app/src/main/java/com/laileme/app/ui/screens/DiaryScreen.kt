package com.laileme.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laileme.app.data.entity.DiaryEntry
import com.laileme.app.ui.PeriodUiState
import com.laileme.app.ui.normalizeDate
import com.laileme.app.ui.components.BodyStatusSection
import com.laileme.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// 心情选项
private data class MoodItem(val key: String, val icon: ImageVector, val label: String)
private val diaryMoodOptions = listOf(
    MoodItem("happy", Icons.Outlined.SentimentSatisfied, "开心"),
    MoodItem("love", Icons.Outlined.FavoriteBorder, "幸福"),
    MoodItem("calm", Icons.Outlined.SelfImprovement, "平静"),
    MoodItem("sleepy", Icons.Outlined.Bedtime, "困倦"),
    MoodItem("sad", Icons.Outlined.SentimentDissatisfied, "难过"),
    MoodItem("angry", Icons.Outlined.SentimentVeryDissatisfied, "烦躁"),
    MoodItem("pain", Icons.Outlined.SentimentNeutral, "不适"),
    MoodItem("sick", Icons.Outlined.LocalHospital, "生病"),
    MoodItem("strong", Icons.Outlined.FitnessCenter, "元气"),
    MoodItem("anxious", Icons.Outlined.Psychology, "焦虑")
)

@Composable
fun DiaryScreen(
    uiState: PeriodUiState,
    diaryEntry: DiaryEntry?,
    onDateSelected: (Long) -> Unit,
    onSaveDiary: (DiaryEntry) -> Unit
) {
    val dateFormat = SimpleDateFormat("M月d日 EEEE", Locale.CHINESE)
    val selectedDate = uiState.selectedDate

    // 所有状态
    var mood by remember(selectedDate) { mutableStateOf(diaryEntry?.mood ?: "") }
    var notes by remember(selectedDate) { mutableStateOf(diaryEntry?.notes ?: "") }
    var flowLevel by remember(selectedDate) { mutableIntStateOf(diaryEntry?.flowLevel ?: 0) }
    var flowColor by remember(selectedDate) { mutableStateOf(diaryEntry?.flowColor ?: "") }
    var painLevel by remember(selectedDate) { mutableIntStateOf(diaryEntry?.painLevel ?: 0) }
    var breastPain by remember(selectedDate) { mutableIntStateOf(diaryEntry?.breastPain ?: 0) }
    var digestive by remember(selectedDate) { mutableIntStateOf(diaryEntry?.digestive ?: 0) }
    var backPain by remember(selectedDate) { mutableIntStateOf(diaryEntry?.backPain ?: 0) }
    var headache by remember(selectedDate) { mutableIntStateOf(diaryEntry?.headache ?: 0) }
    var fatigue by remember(selectedDate) { mutableIntStateOf(diaryEntry?.fatigue ?: 0) }
    var skinCondition by remember(selectedDate) { mutableStateOf(diaryEntry?.skinCondition ?: "") }
    var temperature by remember(selectedDate) { mutableStateOf(diaryEntry?.temperature ?: "") }
    var appetite by remember(selectedDate) { mutableIntStateOf(diaryEntry?.appetite ?: 0) }
    var discharge by remember(selectedDate) { mutableStateOf(diaryEntry?.discharge ?: "") }

    val hasContent = mood.isNotEmpty() || notes.isNotEmpty() ||
            flowLevel > 0 || flowColor.isNotEmpty() || painLevel > 0 ||
            breastPain > 0 || digestive > 0 || backPain > 0 ||
            headache > 0 || fatigue > 0 || skinCondition.isNotEmpty() ||
            temperature.isNotEmpty() || appetite > 0 || discharge.isNotEmpty()

    // 保存成功提示
    var showSavedTip by remember { mutableStateOf(false) }
    LaunchedEffect(showSavedTip) {
        if (showSavedTip) {
            kotlinx.coroutines.delay(1500)
            showSavedTip = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(bottom = 60.dp)
    ) {
        // ── 顶部标题栏 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(PrimaryPink.copy(alpha = 0.08f), Background)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    "今日记录",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                // 日期导航
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 前一天
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = "前一天",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDate
                                    add(Calendar.DAY_OF_MONTH, -1)
                                }
                                onDateSelected(normalizeDate(cal.timeInMillis))
                            },
                        tint = PrimaryPink
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // 当前日期
                    Text(
                        dateFormat.format(Date(selectedDate)),
                        fontSize = 14.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // 后一天
                    val todayMs = normalizeDate(System.currentTimeMillis())
                    val isToday = selectedDate >= todayMs
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "后一天",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = !isToday) {
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDate
                                    add(Calendar.DAY_OF_MONTH, 1)
                                }
                                onDateSelected(normalizeDate(cal.timeInMillis))
                            },
                        tint = if (isToday) TextHint else PrimaryPink
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // 回到今天
                    if (selectedDate != todayMs) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onDateSelected(todayMs) },
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryPink.copy(alpha = 0.12f)
                        ) {
                            Text(
                                "今天",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = PrimaryPink,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    // 已保存标识
                    if (diaryEntry != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = AccentTeal
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("已保存", fontSize = 10.sp, color = AccentTeal)
                        }
                    }
                }
            }
        }

        // ── 内容区域 ──
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── 心情卡片 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEmotions,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = AccentOrange
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "今日心情",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // 两行排列心情选项
                    val firstRow = diaryMoodOptions.take(5)
                    val secondRow = diaryMoodOptions.drop(5)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        firstRow.forEach { moodOpt ->
                            MoodChip(
                                icon = moodOpt.icon,
                                label = moodOpt.label,
                                isSelected = mood == moodOpt.key,
                                onClick = { mood = if (mood == moodOpt.key) "" else moodOpt.key }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        secondRow.forEach { moodOpt ->
                            MoodChip(
                                icon = moodOpt.icon,
                                label = moodOpt.label,
                                isSelected = mood == moodOpt.key,
                                onClick = { mood = if (mood == moodOpt.key) "" else moodOpt.key }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── 身体状态卡片 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    BodyStatusSection(
                        flowLevel = flowLevel, onFlowLevelChange = { flowLevel = it },
                        flowColor = flowColor, onFlowColorChange = { flowColor = it },
                        painLevel = painLevel, onPainLevelChange = { painLevel = it },
                        breastPain = breastPain, onBreastPainChange = { breastPain = it },
                        digestive = digestive, onDigestiveChange = { digestive = it },
                        backPain = backPain, onBackPainChange = { backPain = it },
                        headache = headache, onHeadacheChange = { headache = it },
                        fatigue = fatigue, onFatigueChange = { fatigue = it },
                        skinCondition = skinCondition, onSkinConditionChange = { skinCondition = it },
                        temperature = temperature, onTemperatureChange = { temperature = it },
                        appetite = appetite, onAppetiteChange = { appetite = it },
                        discharge = discharge, onDischargeChange = { discharge = it },
                        initialExpanded = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── 日记卡片 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = PrimaryPink
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "今日日记",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("记录今天的心情和感受吧~", fontSize = 13.sp, color = TextHint) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPink,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color(0xFFFFFAFB),
                            unfocusedContainerColor = Color(0xFFFAFAFA)
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, lineHeight = 20.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 保存按钮 ──
            Button(
                onClick = {
                    onSaveDiary(
                        DiaryEntry(
                            date = selectedDate,
                            mood = mood,
                            notes = notes,
                            flowLevel = flowLevel,
                            flowColor = flowColor,
                            painLevel = painLevel,
                            breastPain = breastPain,
                            digestive = digestive,
                            backPain = backPain,
                            headache = headache,
                            fatigue = fatigue,
                            skinCondition = skinCondition,
                            temperature = temperature,
                            appetite = appetite,
                            discharge = discharge
                        )
                    )
                    showSavedTip = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                enabled = hasContent
            ) {
                Icon(
                    imageVector = if (diaryEntry != null) Icons.Outlined.Edit else Icons.Outlined.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (diaryEntry != null) "更新记录" else "保存记录",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 保存成功提示
            if (showSavedTip) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = AccentTeal.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AccentTeal
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("保存成功~", fontSize = 13.sp, color = AccentTeal, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun MoodChip(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) PrimaryPink.copy(alpha = 0.2f)
                    else Color(0xFFF5F5F5)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = if (isSelected) PrimaryPink else TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            label,
            fontSize = 9.sp,
            color = if (isSelected) PrimaryPink else TextHint,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
