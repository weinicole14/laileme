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
    onMonthChange: (Int) -> Unit,
    onSaveDiary: (Long, String, String, String) -> Unit
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
            DiarySection(
                selectedDate = uiState.selectedDate,
                diaryEntry = diaryEntry,
                onSave = onSaveDiary
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            ToolBar()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BunnyMascot(modifier = Modifier.size(56.dp, 68.dp))
                CountdownBubble(uiState)
            }
        }
    }
}

@Composable
private fun ToolBar() {
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
            ToolBarIcon(Icons.Outlined.Menu, false)
            ToolBarIcon(Icons.Outlined.CalendarMonth, true)
            ToolBarIcon(Icons.Outlined.DarkMode, false)
            ToolBarIcon(Icons.Outlined.ChatBubbleOutline, false)
        }
    }
}

@Composable
private fun ToolBarIcon(icon: ImageVector, isSelected: Boolean) {
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
private fun DiarySection(
    selectedDate: Long,
    diaryEntry: DiaryEntry?,
    onSave: (Long, String, String, String) -> Unit
) {
    val dateFormat = SimpleDateFormat("M月d日", Locale.CHINESE)
    var mood by remember(selectedDate) { mutableStateOf(diaryEntry?.mood ?: "") }
    var notes by remember(selectedDate) { mutableStateOf(diaryEntry?.notes ?: "") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = PrimaryPink
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${dateFormat.format(Date(selectedDate))} 的日记",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                if (diaryEntry != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = AccentTeal
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("已保存", fontSize = 10.sp, color = AccentTeal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 心情选择
            Text("今日心情", fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                moodOptions.forEach { moodOpt ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { mood = if (mood == moodOpt.key) "" else moodOpt.key },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (mood == moodOpt.key) PrimaryPink.copy(alpha = 0.2f)
                                    else Color(0xFFF5F5F5)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = moodOpt.icon,
                                contentDescription = moodOpt.label,
                                modifier = Modifier.size(20.dp),
                                tint = if (mood == moodOpt.key) PrimaryPink else TextSecondary
                            )
                        }
                        Text(
                            moodOpt.label,
                            fontSize = 8.sp,
                            color = if (mood == moodOpt.key) PrimaryPink else TextHint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 日记内容
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("写点什么吧~", fontSize = 12.sp, color = TextHint) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPink,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color(0xFFFFFAFB),
                    unfocusedContainerColor = Color(0xFFFAFAFA)
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 保存按钮
            Button(
                onClick = { onSave(selectedDate, mood, "", notes) },
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                enabled = mood.isNotEmpty() || notes.isNotEmpty()
            ) {
                Icon(
                    imageVector = if (diaryEntry != null) Icons.Outlined.Edit else Icons.Outlined.Save,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (diaryEntry != null) "更新日记" else "保存日记",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
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
