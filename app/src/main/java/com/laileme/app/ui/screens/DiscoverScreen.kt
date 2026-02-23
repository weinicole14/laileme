package com.laileme.app.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laileme.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── 数据类 ──
private data class Medication(
    val name: String,
    val dosage: String,
    val time: String,
    val repeatType: String = "daily",  // daily, weekly, date, once, before_period
    val repeatData: String = ""        // weekly:"1,3,5" date/once:"2024-01-15" before_period:"3"
)

// ── 数据持久化工具 ──
private fun loadMedications(prefs: SharedPreferences): List<Medication> {
    val raw = prefs.getString("medications", "") ?: ""
    if (raw.isEmpty()) return emptyList()
    return raw.split(";;").mapNotNull {
        val parts = it.split("||")
        when (parts.size) {
            3 -> Medication(parts[0], parts[1], parts[2])  // 兼容旧数据
            5 -> Medication(parts[0], parts[1], parts[2], parts[3], parts[4])
            else -> null
        }
    }
}

private fun saveMedications(prefs: SharedPreferences, meds: List<Medication>) {
    prefs.edit().putString(
        "medications",
        meds.joinToString(";;") { "${it.name}||${it.dosage}||${it.time}||${it.repeatType}||${it.repeatData}" }
    ).apply()
}

// ── 提醒频率显示文字 ──
private fun repeatTypeLabel(repeatType: String, repeatData: String): String {
    return when (repeatType) {
        "daily" -> "每天提醒"
        "weekly" -> {
            val days = repeatData.split(",").mapNotNull { it.toIntOrNull() }
            val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")
            if (days.size == 7) "每天提醒"
            else "每周" + days.sorted().map { dayNames.getOrElse(it - 1) { "?" } }.joinToString("、")
        }
        "date" -> "每月${repeatData}号"
        "once" -> "仅一次 · $repeatData"
        "before_period" -> "经期前${repeatData}天"
        else -> "每天提醒"
    }
}

private fun calculateSleepDuration(bedtime: String, waketime: String): Pair<Int, Int>? {
    if (bedtime.isEmpty() || waketime.isEmpty()) return null
    return try {
        val bedParts = bedtime.split(":")
        val wakeParts = waketime.split(":")
        val bedMinutes = bedParts[0].toInt() * 60 + bedParts[1].toInt()
        val wakeMinutes = wakeParts[0].toInt() * 60 + wakeParts[1].toInt()
        val totalMinutes = if (wakeMinutes >= bedMinutes) {
            wakeMinutes - bedMinutes
        } else {
            (24 * 60 - bedMinutes) + wakeMinutes
        }
        Pair(totalMinutes / 60, totalMinutes % 60)
    } catch (_: Exception) { null }
}

// ══════════════════════════════════════════
// 主页面
// ══════════════════════════════════════════
@Composable
fun DiscoverScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("laileme_discover", Context.MODE_PRIVATE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
            .padding(bottom = 100.dp)
    ) {
        Text(
            text = "发现",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── 秘密花园 板块 ──
        SecretGardenSection()

        Spacer(modifier = Modifier.height(16.dp))

        // ── 用药提醒 ──
        MedicationReminderSection(prefs)

        Spacer(modifier = Modifier.height(16.dp))

        // ── 喝水提醒 ──
        WaterReminderSection(prefs)

        Spacer(modifier = Modifier.height(16.dp))

        // ── 睡眠监测 ──
        SleepMonitorSection(prefs)
    }
}

// ══════════════════════════════════════════
// 秘密花园（保留原有）
// ══════════════════════════════════════════
@Composable
private fun SecretGardenSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionIcon(Icons.Outlined.Spa, Color(0xFF66BB6A), Color(0xFFE8F5E9))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("秘密花园", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("属于你的私密空间", fontSize = 11.sp, color = TextHint)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GardenEntryItem(Icons.Outlined.Lock, "私密日记", PrimaryPink)
                GardenEntryItem(Icons.Outlined.PhotoLibrary, "心情相册", AccentOrange)
                GardenEntryItem(Icons.Outlined.MusicNote, "放松音乐", AccentBlue)
                GardenEntryItem(Icons.Outlined.FormatQuote, "暖心语录", AccentTeal)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF3E0)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp), tint = AccentOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("今日花语", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                        Text(
"每一朵花都有自己的花期，不必着急，你终将绽放",
                            fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════
// 用药提醒
// ══════════════════════════════════════════
@Composable
private fun MedicationReminderSection(prefs: SharedPreferences) {
    var showAddDialog by remember { mutableStateOf(false) }
    var medications by remember { mutableStateOf(loadMedications(prefs)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            // 标题栏
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionIcon(Icons.Outlined.LocalPharmacy, Color(0xFFE91E63), Color(0xFFFCE4EC))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("用药提醒", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("按时服药，健康守护", fontSize = 11.sp, color = TextHint)
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(PrimaryPink.copy(alpha = 0.12f))
                        .clickable { showAddDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Add, "添加", Modifier.size(18.dp), tint = PrimaryPink)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (medications.isEmpty()) {
                // 空状态
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF8F9)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.AddCircleOutline, null,
                            Modifier.size(36.dp), tint = TextHint
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "还没有添加用药记录哦~\n点击右上角 + 添加",
                            fontSize = 13.sp, color = TextHint,
                            textAlign = TextAlign.Center, lineHeight = 20.sp
                        )
                    }
                }
            } else {
                medications.forEachIndexed { index, med ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF8F9)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFFCE4EC), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.AccessTime, null,
                                    Modifier.size(20.dp), tint = Color(0xFFE91E63)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    med.name, fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium, color = TextPrimary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Row {
                                    Text(med.time, fontSize = 12.sp, color = PrimaryPink, fontWeight = FontWeight.Medium)
                                    if (med.dosage.isNotEmpty()) {
                                        Text(" · ${med.dosage}", fontSize = 12.sp, color = TextSecondary)
                                    }
                                }
                                Text(
                                    repeatTypeLabel(med.repeatType, med.repeatData),
                                    fontSize = 11.sp,
                                    color = TextHint
                                )
                            }
                            Icon(
                                Icons.Outlined.Close, "删除",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        medications = medications.toMutableList().also { it.removeAt(index) }
                                        saveMedications(prefs, medications)
                                    },
                                tint = TextHint
                            )
                        }
                    }
                    if (index < medications.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // 添加用药对话框
    if (showAddDialog) {
        AddMedicationDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, dosage, time, repeatType, repeatData ->
                medications = medications + Medication(name, dosage, time, repeatType, repeatData)
                saveMedications(prefs, medications)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String) -> Unit // name, dosage, time, repeatType, repeatData
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("08") }
    var minute by remember { mutableStateOf("00") }
    var repeatType by remember { mutableStateOf("daily") }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var dateDay by remember { mutableStateOf("") }
    var onceDate by remember { mutableStateOf("") }
    var beforeDays by remember { mutableStateOf("3") }

    val repeatOptions = listOf(
        "daily" to "每天",
        "weekly" to "自选周几",
        "once" to "仅一次",
        "date" to "每月特定日期",
        "before_period" to "经期前提醒"
    )
    val weekDayNames = listOf("一", "二", "三", "四", "五", "六", "日")

    // 校验是否可提交
    val canSubmit = name.isNotBlank() && when (repeatType) {
        "weekly" -> selectedDays.isNotEmpty()
        "date" -> dateDay.isNotBlank() && (dateDay.toIntOrNull() ?: 0) in 1..31
        "once" -> onceDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
        "before_period" -> beforeDays.isNotBlank() && (beforeDays.toIntOrNull() ?: 0) > 0
        else -> true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Text("添加用药提醒", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // ── 药品名称 ──
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("药品名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // ── 用量 ──
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("用量（如：1片、5ml）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // ── 服药时间 ──
                Text("服药时间", fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = {
                            val v = it.filter { c -> c.isDigit() }.take(2)
                            if (v.isEmpty() || v.toInt() in 0..23) hour = v
                        },
                        modifier = Modifier.width(70.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                    )
                    Text(" : ", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    OutlinedTextField(
                        value = minute,
                        onValueChange = {
                            val v = it.filter { c -> c.isDigit() }.take(2)
                            if (v.isEmpty() || v.toInt() in 0..59) minute = v
                        },
                        modifier = Modifier.width(70.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── 提醒频率 ──
                Text("提醒频率", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                // 频率选项 chips — 第一行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeatOptions.take(3).forEach { (type, label) ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { repeatType = type },
                            shape = RoundedCornerShape(20.dp),
                            color = if (repeatType == type) PrimaryPink else Color(0xFFF5F5F5)
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                fontSize = 12.sp,
                                color = if (repeatType == type) Color.White else TextSecondary,
                                fontWeight = if (repeatType == type) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                // 频率选项 chips — 第二行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeatOptions.drop(3).forEach { (type, label) ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { repeatType = type },
                            shape = RoundedCornerShape(20.dp),
                            color = if (repeatType == type) PrimaryPink else Color(0xFFF5F5F5)
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                fontSize = 12.sp,
                                color = if (repeatType == type) Color.White else TextSecondary,
                                fontWeight = if (repeatType == type) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 根据频率类型显示额外设置 ──
                when (repeatType) {
                    "weekly" -> {
                        Text("选择提醒的星期", fontSize = 12.sp, color = TextHint)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            weekDayNames.forEachIndexed { index, dayName ->
                                val dayNum = index + 1
                                val isSelected = dayNum in selectedDays
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) PrimaryPink else Color(0xFFF5F5F5)
                                        )
                                        .clickable {
                                            selectedDays = if (isSelected) {
                                                selectedDays - dayNum
                                            } else {
                                                selectedDays + dayNum
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        dayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                        if (selectedDays.isEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("请至少选择一天哦～", fontSize = 11.sp, color = PrimaryPink)
                        }
                    }

                    "date" -> {
                        Text("每月几号提醒", fontSize = 12.sp, color = TextHint)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("每月 ", fontSize = 14.sp, color = TextPrimary)
                            OutlinedTextField(
                                value = dateDay,
                                onValueChange = {
                                    val v = it.filter { c -> c.isDigit() }.take(2)
                                    if (v.isEmpty() || (v.toIntOrNull() ?: 0) in 1..31) dateDay = v
                                },
                                modifier = Modifier.width(65.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                            )
                            Text(" 号", fontSize = 14.sp, color = TextPrimary)
                        }
                    }

                    "once" -> {
                        Text("选择提醒日期（仅提醒一次）", fontSize = 12.sp, color = TextHint)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = onceDate,
                            onValueChange = {
                                // 只允许数字和横线，最长10字符
                                val filtered = it.filter { c -> c.isDigit() || c == '-' }.take(10)
                                onceDate = filtered
                            },
                            label = { Text("格式：2025-01-15") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    "before_period" -> {
                        Text("经期来临前几天开始提醒", fontSize = 12.sp, color = TextHint)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("经期前 ", fontSize = 14.sp, color = TextPrimary)
                            OutlinedTextField(
                                value = beforeDays,
                                onValueChange = {
                                    val v = it.filter { c -> c.isDigit() }.take(2)
                                    beforeDays = v
                                },
                                modifier = Modifier.width(65.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                            )
                            Text(" 天", fontSize = 14.sp, color = TextPrimary)
                        }
                    }

                    // "daily" 不需要额外设置
                    else -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF8F9)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Info, null,
                                    Modifier.size(16.dp), tint = TextHint
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("每天在设定时间提醒你服药～", fontSize = 12.sp, color = TextHint)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (canSubmit) {
                        val h = hour.padStart(2, '0')
                        val m = minute.padStart(2, '0')
                        val time = "$h:$m"
                        val data = when (repeatType) {
                            "weekly" -> selectedDays.sorted().joinToString(",")
                            "date" -> dateDay
                            "once" -> onceDate
                            "before_period" -> beforeDays
                            else -> ""
                        }
                        onAdd(name.trim(), dosage.trim(), time, repeatType, data)
                    }
                },
                enabled = canSubmit
            ) {
                Text("添加", color = if (canSubmit) PrimaryPink else TextHint)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

// ══════════════════════════════════════════
// 喝水提醒
// ══════════════════════════════════════════
@Composable
private fun WaterReminderSection(prefs: SharedPreferences) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val savedDate = prefs.getString("water_date", "") ?: ""

    var cups by remember {
        mutableStateOf(if (savedDate == today) prefs.getInt("water_cups", 0) else 0)
    }
    val goal = remember { prefs.getInt("water_goal", 8) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var currentGoal by remember { mutableStateOf(goal) }

    // 自动保存
    LaunchedEffect(cups) {
        prefs.edit()
            .putString("water_date", today)
            .putInt("water_cups", cups)
            .apply()
    }

    val progress = (cups.toFloat() / currentGoal).coerceIn(0f, 1f)
    val waterColor = Color(0xFF42A5F5)
    val waterBg = Color(0xFFE3F2FD)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionIcon(Icons.Outlined.LocalDrink, waterColor, waterBg)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("喝水提醒", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("每天 ${currentGoal} 杯，保持水润", fontSize = 11.sp, color = TextHint)
                }
                // 设置目标
                Icon(
                    Icons.Outlined.Settings, "设置目标",
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable { showGoalDialog = true },
                    tint = TextHint
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 进度显示
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = waterBg.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 大数字显示
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "$cups",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = waterColor
                        )
                        Text(
                            " / $currentGoal 杯",
                            fontSize = 16.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (cups >= currentGoal) Icons.Outlined.Celebration else Icons.Outlined.WaterDrop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (cups >= currentGoal) AccentTeal else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (cups >= currentGoal) "今日饮水目标已达成！"
                            else "还差 ${currentGoal - cups} 杯就达标啦~",
                            fontSize = 13.sp,
                            color = if (cups >= currentGoal) AccentTeal else TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 进度条
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (cups >= currentGoal) AccentTeal else waterColor,
                        trackColor = Color(0xFFE0E0E0),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 操作按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 减少
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (cups > 0) Color(0xFFFFEBEE) else Color(0xFFF5F5F5))
                                .clickable(enabled = cups > 0) { cups-- },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Remove, "减少",
                                Modifier.size(22.dp),
                                tint = if (cups > 0) PeriodRed else TextHint
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        // 增加（主按钮）
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(waterColor)
                                .clickable { cups++ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Add, "喝了一杯",
                                Modifier.size(28.dp), tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        // 重置
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF5F5F5))
                                .clickable { cups = 0 },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Refresh, "重置",
                                Modifier.size(22.dp), tint = TextHint
                            )
                        }
                    }
                }
            }
        }
    }

    // 目标设置对话框
    if (showGoalDialog) {
        var goalInput by remember { mutableStateOf(currentGoal.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = { Text("设置每日饮水目标", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("建议每天饮用 6-10 杯水（每杯约250ml）", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = {
                            val v = it.filter { c -> c.isDigit() }.take(2)
                            goalInput = v
                        },
                        label = { Text("杯数") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        suffix = { Text("杯") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = goalInput.toIntOrNull()
                    if (v != null && v in 1..20) {
                        currentGoal = v
                        prefs.edit().putInt("water_goal", v).apply()
                        showGoalDialog = false
                    }
                }) { Text("确定", color = PrimaryPink) }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }
}

// ══════════════════════════════════════════
// 睡眠监测
// ══════════════════════════════════════════
@Composable
private fun SleepMonitorSection(prefs: SharedPreferences) {
    val context = LocalContext.current
    val db = remember { com.laileme.app.data.AppDatabase.getDatabase(context) }
    val sleepDao = remember { db.sleepDao() }
    val scope = rememberCoroutineScope()
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var bedtime by remember { mutableStateOf("") }
    var waketime by remember { mutableStateOf("") }
    var dbLoaded by remember { mutableStateOf(false) }

    // 从数据库加载今日记录
    LaunchedEffect(today) {
        val record = sleepDao.getByDate(today)
        bedtime = record?.bedtime ?: ""
        waketime = record?.waketime ?: ""
        dbLoaded = true
    }

    val duration = calculateSleepDuration(bedtime, waketime)
    val sleepColor = Color(0xFF5C6BC0)
    val sleepBg = Color(0xFFE8EAF6)

    // 获取当前时间的工具函数
    fun getCurrentTimeStr(): String {
        val cal = Calendar.getInstance()
        return String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    // 保存到数据库
    fun saveSleep(newBedtime: String, newWaketime: String) {
        scope.launch {
            sleepDao.insert(
                com.laileme.app.data.entity.SleepRecord(
                    date = today,
                    bedtime = newBedtime,
                    waketime = newWaketime
                )
            )
        }
    }

    // 睡眠质量评估
    val (qualityIcon, qualityText, qualityColor) = when {
        duration == null -> Triple(Icons.Outlined.Bedtime, "记录睡眠吧", TextHint)
        duration.first < 6 -> Triple(Icons.Outlined.SentimentVeryDissatisfied, "睡眠不足", PeriodRed)
        duration.first < 7 -> Triple(Icons.Outlined.SentimentNeutral, "还可以哦", AccentOrange)
        duration.first < 9 -> Triple(Icons.Outlined.SentimentVerySatisfied, "很棒！", AccentTeal)
        else -> Triple(Icons.Outlined.SentimentDissatisfied, "睡太多啦", AccentOrange)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionIcon(Icons.Outlined.Bedtime, sleepColor, sleepBg)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("睡眠监测", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("好好休息，元气满满", fontSize = 11.sp, color = TextHint)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 睡眠时间卡片
            Row(modifier = Modifier.fillMaxWidth()) {
                // 入睡时间 — 点击自动获取当前时间
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val now = getCurrentTimeStr()
                            bedtime = now
                            saveSleep(now, waketime)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF3E5F5)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.NightsStay, null,
                            Modifier.size(22.dp), tint = Color(0xFF9C27B0)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("入睡", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            if (bedtime.isEmpty()) "点击记录" else bedtime,
                            fontSize = if (bedtime.isEmpty()) 13.sp else 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bedtime.isEmpty()) TextHint else Color(0xFF9C27B0)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 起床时间 — 点击自动获取当前时间
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val now = getCurrentTimeStr()
                            waketime = now
                            saveSleep(bedtime, now)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF8E1)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.WbSunny, null,
                            Modifier.size(22.dp), tint = AccentOrange
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("起床", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            if (waketime.isEmpty()) "点击记录" else waketime,
                            fontSize = if (waketime.isEmpty()) 13.sp else 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (waketime.isEmpty()) TextHint else AccentOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 睡眠时长 & 质量
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = sleepBg.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(qualityIcon, contentDescription = null, modifier = Modifier.size(28.dp), tint = qualityColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (duration != null) {
                            Text(
                                "睡了 ${duration.first} 小时 ${duration.second} 分钟",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        } else {
                            Text(
                                "尚未记录今日睡眠",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                        Text(
                            qualityText,
                            fontSize = 13.sp,
                            color = qualityColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (duration != null) {
                        // 睡眠进度（以8小时为满分）
                        val sleepProgress = ((duration.first * 60 + duration.second) / 480f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(qualityColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${(sleepProgress * 100).toInt()}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = qualityColor
                            )
                        }
                    }
                }
            }

            // 建议
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = TextHint
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "建议每晚 22:00~23:00 入睡，保持 7~8 小时的充足睡眠",
                    fontSize = 11.sp,
                    color = TextHint,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun TimeInputDialog(
    title: String,
    initialHour: String,
    initialMinute: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = { Text(title, fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = hour,
                    onValueChange = {
                        val v = it.filter { c -> c.isDigit() }.take(2)
                        if (v.isEmpty() || v.toInt() in 0..23) hour = v
                    },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    label = { Text("时") }
                )
                Text(
                    " : ",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                OutlinedTextField(
                    value = minute,
                    onValueChange = {
                        val v = it.filter { c -> c.isDigit() }.take(2)
                        if (v.isEmpty() || v.toInt() in 0..59) minute = v
                    },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    label = { Text("分") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = hour.padStart(2, '0')
                val m = minute.padStart(2, '0')
                onConfirm(h, m)
            }) { Text("确定", color = PrimaryPink) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        }
    )
}

// ══════════════════════════════════════════
// 通用辅助组件
// ══════════════════════════════════════════
@Composable
private fun SectionIcon(icon: ImageVector, iconColor: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(bgColor, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = iconColor)
    }
}

@Composable
private fun GardenEntryItem(icon: ImageVector, label: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { /* TODO */ }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, Modifier.size(22.dp), tint = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
    }
}
