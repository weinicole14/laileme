package com.laileme.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laileme.app.data.entity.SecretRecord
import com.laileme.app.ui.SecretUiState
import java.text.SimpleDateFormat
import java.util.*

private val SecretBackground = Color(0xFFFAF5FA)
private val SecretPrimary = Color(0xFFC084B6)
private val SecretCardBg = Color.White
private val SecretText = Color(0xFF4A3B47)

@Composable
fun SecretDiaryScreen(
    uiState: SecretUiState,
    onBack: () -> Unit,
    onDateSelected: (Long) -> Unit,
    onMonthChange: (Int) -> Unit,
    onSaveRecord: (SecretRecord) -> Unit,
    onDeleteRecord: (Long) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SecretBackground)
            .statusBarsPadding()
    ) {
        // 顶部导航
        Surface(
            color = SecretBackground,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = SecretText)
                }
                Text(
                    "私密日记",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecretText,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp)) // 占位保持居中
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // 私密日历
            SecretCalendarView(
                uiState = uiState,
                onMonthChange = onMonthChange,
                onDateSelected = onDateSelected
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 记录卡片
            SecretRecordCard(
                selectedDate = uiState.selectedDate,
                record = uiState.currentRecord,
                defaultHadSex = uiState.defaultHadSex,
                onSave = onSaveRecord,
                onDelete = { onDeleteRecord(uiState.selectedDate) }
            )
            
            // 留出底部导航栏足够的间距
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun SecretCalendarView(
    uiState: SecretUiState,
    onMonthChange: (Int) -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val cal = uiState.currentMonth
    val monthYearFormat = SimpleDateFormat("yyyy年M月", Locale.CHINESE)
    val monthStr = monthYearFormat.format(cal.time)

    // 计算当月天数和第一天是周几
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = 星期日
    val offset = if (firstDayOfWeek == 1) 6 else firstDayOfWeek - 2 // 调整为周一开头

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SecretCardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 月份切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChange(-1) }) {
                    Icon(Icons.Default.ChevronLeft, "上个月", tint = SecretText)
                }
                Text(monthStr, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SecretText)
                IconButton(onClick = { onMonthChange(1) }) {
                    Icon(Icons.Default.ChevronRight, "下个月", tint = SecretText)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 星期表头
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                    Text(day, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 日期网格
            val today = Calendar.getInstance()
            val todayYear = today.get(Calendar.YEAR)
            val todayDayOfYear = today.get(Calendar.DAY_OF_YEAR)

            val totalCells = Math.ceil((daysInMonth + offset) / 7.0).toInt() * 7

            Column {
                for (row in 0 until (totalCells / 7)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        for (col in 0..6) {
                            val dayNum = row * 7 + col - offset + 1
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNum in 1..daysInMonth) {
                                    val cellCal = cal.clone() as Calendar
                                    cellCal.set(Calendar.DAY_OF_MONTH, dayNum)
                                    val dateStr = com.laileme.app.ui.normalizeDate(cellCal.timeInMillis)

                                    val isSelected = dateStr == uiState.selectedDate
                                    val isToday = cellCal.get(Calendar.YEAR) == todayYear && cellCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear

                                    // 检查是否在记录中
                                    val record = uiState.records.find { it.date == dateStr }
                                    val hasRecord = record != null

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) SecretPrimary else if (isToday) SecretPrimary.copy(alpha = 0.1f) else Color.Transparent
                                            )
                                            .clickable { onDateSelected(dateStr) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                dayNum.toString(),
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else SecretText
                                            )
                                            if (hasRecord) {
                                                Icon(
                                                    if (record!!.hadSex) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color.White else SecretPrimary,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecretRecordCard(
    selectedDate: Long,
    record: SecretRecord?,
    defaultHadSex: Boolean = false,
    onSave: (SecretRecord) -> Unit,
    onDelete: () -> Unit
) {
    // 如果当天有记录就用记录的值，否则继承最近一次的爱爱状态
    var hadSex by remember(selectedDate, record, defaultHadSex) { mutableStateOf(record?.hadSex ?: defaultHadSex) }
    var protection by remember(selectedDate, record) { mutableStateOf(record?.protection ?: "") }
    var feeling by remember(selectedDate, record) { mutableIntStateOf(record?.feeling ?: 0) }
    var mood by remember(selectedDate, record) { mutableStateOf(record?.mood ?: "") }
    var notes by remember(selectedDate, record) { mutableStateOf(record?.notes ?: "") }

    var showProtectionWarning by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE)
    val dateText = dateFormat.format(Date(selectedDate))

    // 温馨提醒弹窗
    if (showProtectionWarning) {
        AlertDialog(
            onDismissRequest = { showProtectionWarning = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("暖心提醒(｡♥‿♥｡)", color = SecretPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    "女孩子一定要保护好自己哦！不要轻易相信别人，任何时候都要把自己的安全和健康放在第一位。\n\n宝贝，你真的准备好了吗？",
                    fontSize = 14.sp,
                    color = SecretText,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    hadSex = true
                    showProtectionWarning = false
                }) {
                    Text("我准备好了", color = SecretPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showProtectionWarning = false
                }) {
                    Text("我再想想", color = Color.Gray)
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SecretCardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dateText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SecretText)
                if (record != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.Delete, "删除记录", tint = Color.Gray)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 爱爱开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Favorite, "爱爱", tint = SecretPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("记录爱爱", fontSize = 15.sp, color = SecretText)
                }
                Switch(
                    checked = hadSex,
                    onCheckedChange = { checked ->
                        if (checked && !hadSex) {
                            // 开启时弹出暖心提醒
                            showProtectionWarning = true
                        } else {
                            hadSex = checked
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SecretPrimary)
                )
            }

            AnimatedVisibility(visible = hadSex) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("避孕方式", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val protections = listOf(
                        "" to "未记录",
                        "none" to "无避孕",
                        "condom" to "安全套",
                        "pill" to "避孕药",
                        "safe_period" to "安全期体外",
                        "other" to "其他"
                    )
                    
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        protections.forEach { (key, label) ->
                            val isSel = protection == key
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSel) SecretPrimary else Color(0xFFF5F5F5),
                                modifier = Modifier.clickable { protection = key }
                            ) {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    color = if (isSel) Color.White else SecretText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // 各种避孕方式的温馨提示
                    val protectionTip = when (protection) {
                        "safe_period" -> "⚠ 安全期和体外并不安全哦，失败率较高，建议搭配其他避孕方式~"
                        "none" -> "⚠ 宝贝请一定要做好保护措施，爱自己才能更好地爱别人哦！"
                        "condom" -> "💕 记得使用前检查好有没有破损哦，注意全程佩戴~"
                        "pill" -> "💊 记得按时吃药哦，不要漏服，注意身体反应~"
                        else -> null
                    }
                    AnimatedVisibility(visible = protectionTip != null) {
                        Text(
                            protectionTip ?: "",
                            fontSize = 12.sp,
                            color = if (protection == "safe_period" || protection == "none") Color(0xFFE57373) else SecretPrimary,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("愉悦度", fontSize = 13.sp, color = Color.Gray)
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        for (i in 1..5) {
                Icon(
                    if (i <= feeling) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "星级 $i",
                    tint = if (i <= feeling) SecretPrimary else Color.LightGray,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { feeling = i }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(20.dp))

            // 私密日记
            Text("私密日记", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SecretText)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = mood,
                onValueChange = { mood = it },
                label = { Text("今天心情", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SecretPrimary,
                    focusedLabelColor = SecretPrimary,
                    cursorColor = SecretPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("写点只有自己知道的小秘密...", fontSize = 12.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SecretPrimary,
                    focusedLabelColor = SecretPrimary,
                    cursorColor = SecretPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    onSave(
                        SecretRecord(
                            date = selectedDate,
                            hadSex = hadSex,
                            protection = protection,
                            feeling = feeling,
                            mood = mood,
                            notes = notes
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SecretPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("保存私密记录", fontWeight = FontWeight.Bold)
            }
        }
    }
}