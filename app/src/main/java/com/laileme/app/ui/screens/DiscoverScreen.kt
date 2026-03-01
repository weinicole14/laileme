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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laileme.app.ui.SecretViewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

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
    com.laileme.app.data.SyncManager.triggerImmediateSync()
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

// ── 习惯打卡数据类 ──
private data class Habit(
    val id: String,
    val name: String,
    val iconIndex: Int = 0,
    val colorIndex: Int = 0,
    val frequency: String = "daily",  // daily, weekly
    val frequencyData: String = "",   // weekly: "1,3,5"
    val isCouple: Boolean = false
)

private val habitIconOptions: List<Pair<ImageVector, String>>
    @Composable get() = listOf(
        Icons.Outlined.FitnessCenter to "运动",
        Icons.Outlined.MenuBook to "阅读",
        Icons.Outlined.SelfImprovement to "冥想",
        Icons.Outlined.DirectionsRun to "跑步",
        Icons.Outlined.Brush to "画画",
        Icons.Outlined.MusicNote to "音乐",
        Icons.Outlined.Favorite to "打卡",
        Icons.Outlined.Star to "目标",
        Icons.Outlined.LocalDrink to "饮食",
        Icons.Outlined.Bedtime to "早睡"
    )

private val habitColorOptions = listOf(
    Color(0xFFFF7043), Color(0xFF42A5F5), Color(0xFF66BB6A), Color(0xFFAB47BC),
    Color(0xFFEF5350), Color(0xFF26C6DA), Color(0xFFFFCA28), Color(0xFF8D6E63),
    Color(0xFFEC407A), Color(0xFF5C6BC0)
)

private fun loadHabits(prefs: SharedPreferences): List<Habit> {
    val raw = prefs.getString("habits", "") ?: ""
    if (raw.isEmpty()) return emptyList()
    return raw.split(";;").mapNotNull {
        val parts = it.split("||")
        if (parts.size >= 7) {
            Habit(parts[0], parts[1], parts[2].toIntOrNull() ?: 0, parts[3].toIntOrNull() ?: 0,
                  parts[4], parts[5], parts[6] == "true")
        } else null
    }
}

private fun saveHabits(prefs: SharedPreferences, habits: List<Habit>) {
    prefs.edit().putString("habits",
        habits.joinToString(";;") {
            "${it.id}||${it.name}||${it.iconIndex}||${it.colorIndex}||${it.frequency}||${it.frequencyData}||${it.isCouple}"
        }
    ).apply()
    com.laileme.app.data.SyncManager.triggerImmediateSync()
}

private fun loadCheckins(prefs: SharedPreferences): Map<String, Set<String>> {
    val raw = prefs.getString("habit_checkins", "") ?: ""
    if (raw.isEmpty()) return emptyMap()
    return raw.split(";;").filter { it.contains("::") }.mapNotNull {
        val parts = it.split("::", limit = 2)
        if (parts.size == 2 && parts[1].isNotEmpty()) parts[0] to parts[1].split(",").toSet()
        else if (parts.size == 2) parts[0] to emptySet()
        else null
    }.toMap()
}

private fun saveCheckins(prefs: SharedPreferences, checkins: Map<String, Set<String>>) {
    prefs.edit().putString("habit_checkins",
        checkins.entries.filter { it.value.isNotEmpty() }
            .joinToString(";;") { "${it.key}::${it.value.joinToString(",")}" }
    ).apply()
    com.laileme.app.data.SyncManager.triggerImmediateSync()
}

private fun getStreak(checkins: Set<String>?): Int {
    if (checkins.isNullOrEmpty()) return 0
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    var streak = 0
    val today = sdf.format(cal.time)
    if (today !in checkins) {
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    while (true) {
        val dateStr = sdf.format(cal.time)
        if (dateStr in checkins) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else break
    }
    return streak
}

private fun shouldCheckToday(habit: Habit): Boolean {
    if (habit.frequency == "daily") return true
    if (habit.frequency == "weekly") {
        val cal = Calendar.getInstance()
        val dayOfWeek = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1 // 1=周一...7=周日
        val days = habit.frequencyData.split(",").mapNotNull { it.toIntOrNull() }
        return dayOfWeek in days
    }
    return true
}

private fun calculateSleepDuration(bedtime: String, waketime: String): Pair<Int, Int>? {
    if (bedtime.isEmpty() || waketime.isEmpty()) return null
    return try {
        val bedParts = bedtime.split(":")
        val wakeParts = waketime.split(":")
        val bedMinutes = bedParts[0].toInt() * 60 + bedParts[1].toInt()
        val wakeMinutes = wakeParts[0].toInt() * 60 + wakeParts[1].toInt()
        val diffMinutes = if (wakeMinutes >= bedMinutes) {
            wakeMinutes - bedMinutes
        } else {
            (24 * 60 - bedMinutes) + wakeMinutes
        }
        // 超过18小时说明操作顺序反了，取较短解释
        val totalMinutes = if (diffMinutes > 18 * 60) (24 * 60 - diffMinutes) else diffMinutes
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
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // 各板块位置（像素）
    val sectionPositions = remember { mutableStateMapOf<Int, Int>() }

    // 快捷导航数据：index, icon, label, color
    data class NavShortcut(val index: Int, val icon: ImageVector, val label: String, val color: Color)
    val shortcuts = remember { listOf(
        NavShortcut(0, Icons.Outlined.Spa, "花语", Color(0xFF66BB6A)),
        NavShortcut(1, Icons.Outlined.LocalPharmacy, "用药", Color(0xFFE91E63)),
        NavShortcut(2, Icons.Outlined.LocalDrink, "喝水", Color(0xFF42A5F5)),
        NavShortcut(3, Icons.Outlined.Bedtime, "睡眠", Color(0xFF5C6BC0)),
        NavShortcut(4, Icons.Outlined.Timer, "专注", Color(0xFFFF7043)),
        NavShortcut(5, Icons.Outlined.CheckCircle, "习惯", Color(0xFFAB47BC))
    ) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(12.dp)
                .padding(bottom = 100.dp)
        ) {
            Text(
                text = "发现",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── 快捷导航栏 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                shortcuts.forEach { shortcut ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val pos = sectionPositions[shortcut.index] ?: 0
                                scope.launch { scrollState.animateScrollTo(pos) }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(shortcut.color.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                shortcut.icon, shortcut.label,
                                Modifier.size(20.dp), tint = shortcut.color
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            shortcut.label,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
    
            // ── 今日花语 板块 ──
            Box(modifier = Modifier.onGloballyPositioned {
                sectionPositions[0] = it.positionInParent().y.toInt()
            }) { FlowerLanguageSection() }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 用药提醒 ──
            Box(modifier = Modifier.onGloballyPositioned {
                sectionPositions[1] = it.positionInParent().y.toInt()
            }) { MedicationReminderSection(prefs) }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 喝水提醒 ──
            Box(modifier = Modifier.onGloballyPositioned {
                sectionPositions[2] = it.positionInParent().y.toInt()
            }) { WaterReminderSection(prefs) }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 睡眠监测 ──
            Box(modifier = Modifier.onGloballyPositioned {
                sectionPositions[3] = it.positionInParent().y.toInt()
            }) { SleepMonitorSection(prefs) }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 专注模式（番茄钟）──
            Box(modifier = Modifier.onGloballyPositioned {
                sectionPositions[4] = it.positionInParent().y.toInt()
            }) { FocusModeSection(prefs) }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 习惯打卡 ──
            Box(modifier = Modifier.onGloballyPositioned {
                sectionPositions[5] = it.positionInParent().y.toInt()
            }) { HabitTrackerSection(prefs) }
        }
    }
}

// ══════════════════════════════════════════
// 今日花语
// ══════════════════════════════════════════
@Composable
private fun FlowerLanguageSection() {
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
                    Text("今日花语", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("每天一句温暖的话", fontSize = 11.sp, color = TextHint)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF3E0)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(20.dp), tint = AccentOrange)
                    Spacer(modifier = Modifier.width(10.dp))
                    val flowerQuotes = remember { listOf(
                        "每一朵花都有自己的花期，不必着急，你终将绽放",
                        "你是这世界上独一无二的花，值得被温柔以待",
                        "风会记住一朵花的香，我会记住你的好",
                        "向日葵告诉我，只要面向太阳，就不会看到阴影",
                        "玫瑰不急不躁，该开的时候自然会开",
                        "雏菊虽小，也有自己的春天",
                        "像栀子花一样，默默散发芬芳",
                        "你的笑容，比樱花更让人心动",
                        "薰衣草说：等待也是一种温柔",
                        "愿你如莲花般，出淤泥而不染",
                        "茉莉虽朴素，香气却能飘满整个房间",
                        "做一朵野花吧，自由自在地盛开",
                        "铃兰花语：幸福归来，一切都会好的",
                        "你是别人眼中最美的那朵花",
                        "花不会因为没人欣赏就不开放",
                        "桃花说：春天来了，好事也在路上",
                        "蒲公英虽然飘散，每一颗都带着希望",
                        "百合代表纯洁，愿你永远保持一颗纯净的心",
                        "紫罗兰告诉你：永恒的美存在于内心",
                        "康乃馨说：感恩身边每一个爱你的人",
                        "勿忘我：真正重要的人，心里一直都记得",
                        "绣球花语：希望、团聚，你并不孤单",
                        "三色堇说：思念是最温柔的牵挂",
                        "水仙花语：自爱是一切爱的起点",
                        "芍药告诉你：温柔是最强大的力量",
                        "郁金香说：值得等待的爱，终会来到",
                        "满天星的陪伴，虽渺小却无处不在",
                        "梅花香自苦寒来，你的坚持终会有回报",
                        "牡丹不争春，盛开时自有万千风华",
                        "风信子说：只要重新开始，永远都不晚",
                        "一束阳光，一朵花，一份好心情就足够了"
                    ) }
                    val todayIndex = remember {
                        val cal = java.util.Calendar.getInstance()
                        cal.get(java.util.Calendar.DAY_OF_YEAR) % flowerQuotes.size
                    }
                    Text(
                        flowerQuotes[todayIndex],
                        fontSize = 13.sp, color = TextSecondary, lineHeight = 20.sp
                    )
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

            // 水杯进度显示
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = progress,
                    animationSpec = androidx.compose.animation.core.tween(600),
                    label = "water"
                )
                val fillColor = if (cups >= currentGoal) AccentTeal else waterColor

                // 水波纹动画
                val pi2 = (2.0 * Math.PI).toFloat()
                val waveTransition = rememberInfiniteTransition(label = "wave")
                val wavePhase by waveTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = pi2,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "wavePhase"
                )

                // 水杯
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val w = size.width
                        val h = size.height
                        val stroke = 2.5f
                        val r = 18f  // 底部圆角半径

                        // 杯子区域：上宽下窄，有角度的杯壁
                        val topL = w * 0.12f     // 杯口左
                        val topR = w * 0.88f     // 杯口右
                        val botL = w * 0.22f     // 杯底左
                        val botR = w * 0.78f     // 杯底右
                        val cT = h * 0.06f       // 杯口顶
                        val cB = h * 0.90f       // 杯底

                        // 辅助函数：给定 Y 坐标，插值出左右杯壁 X
                        fun cupLeftAt(y: Float): Float {
                            val t = ((y - cT) / (cB - cT)).coerceIn(0f, 1f)
                            return topL + (botL - topL) * t
                        }
                        fun cupRightAt(y: Float): Float {
                            val t = ((y - cT) / (cB - cT)).coerceIn(0f, 1f)
                            return topR + (botR - topR) * t
                        }

                        // ── 杯身闭合路径 ──
                        val cupClipPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(topL, cT)
                            // 左壁斜线到圆角起点
                            lineTo(botL, cB - r)
                            // 左下圆角
                            quadraticBezierTo(botL, cB, botL + r, cB)
                            // 平底
                            lineTo(botR - r, cB)
                            // 右下圆角
                            quadraticBezierTo(botR, cB, botR, cB - r)
                            // 右壁斜线回顶部
                            lineTo(topR, cT)
                            close()
                        }

                        // ── 杯身淡背景 ──
                        drawPath(cupClipPath, Color(0x0D000000))

                        // ── 水填充 ──
                        if (animatedProgress > 0f) {
                            val cupInnerH = cB - cT
                            val waterY = cT + cupInnerH * (1f - animatedProgress)
                            val wL = cupLeftAt(waterY)
                            val wR = cupRightAt(waterY)

                            val waterPath = androidx.compose.ui.graphics.Path().apply {
                                // 水面波浪线
                                moveTo(wL, waterY + kotlin.math.sin(wavePhase.toDouble()).toFloat() * 3f)
                                val steps = 20
                                for (i in 1..steps) {
                                    val frac = i.toFloat() / steps
                                    val x = wL + (wR - wL) * frac
                                    val y = waterY + kotlin.math.sin((wavePhase + frac * pi2).toDouble()).toFloat() * 3f
                                    lineTo(x, y)
                                }
                                // 右壁到底部圆角
                                if (waterY < cB - r) {
                                    lineTo(botR, cB - r)
                                }
                                quadraticBezierTo(botR, cB, botR - r, cB)
                                // 平底
                                lineTo(botL + r, cB)
                                // 左下圆角
                                quadraticBezierTo(botL, cB, botL, cB - r)
                                // 左壁回水面
                                if (waterY < cB - r) {
                                    lineTo(wL, waterY)
                                }
                                close()
                            }

                            drawPath(
                                waterPath,
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        fillColor.copy(alpha = 0.4f),
                                        fillColor.copy(alpha = 0.7f),
                                        fillColor.copy(alpha = 0.85f)
                                    ),
                                    startY = waterY,
                                    endY = cB
                                )
                            )

                            // 气泡
                            if (animatedProgress > 0.15f) {
                                val bAlpha = 0.4f
                                val midX = (wL + wR) / 2f
                                drawCircle(Color.White.copy(bAlpha), 3.5f,
                                    androidx.compose.ui.geometry.Offset(midX - 12f, waterY + cupInnerH * 0.12f))
                                drawCircle(Color.White.copy(bAlpha), 2f,
                                    androidx.compose.ui.geometry.Offset(midX + 10f, waterY + cupInnerH * 0.22f))
                                drawCircle(Color.White.copy(bAlpha * 0.7f), 2.5f,
                                    androidx.compose.ui.geometry.Offset(midX - 3f, waterY + cupInnerH * 0.38f))
                            }
                        }

                        // ── 杯身轮廓（U 型开口）──
                        val outlinePath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(topL, cT)
                            lineTo(botL, cB - r)
                            quadraticBezierTo(botL, cB, botL + r, cB)
                            lineTo(botR - r, cB)
                            quadraticBezierTo(botR, cB, botR, cB - r)
                            lineTo(topR, cT)
                        }
                        drawPath(
                            outlinePath,
                            Color(0xFFB0BEC5),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = stroke,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )

                        // ── 杯口圆点（厚杯沿）──
                        drawCircle(Color(0xFF90A4AE), 3.5f, androidx.compose.ui.geometry.Offset(topL, cT))
                        drawCircle(Color(0xFF90A4AE), 3.5f, androidx.compose.ui.geometry.Offset(topR, cT))

                        // ── 玻璃高光 ──
                        drawLine(
                            Color.White.copy(alpha = 0.3f),
                            androidx.compose.ui.geometry.Offset(topL + 8f, cT + 18f),
                            androidx.compose.ui.geometry.Offset(botL + 6f, cB - 28f),
                            strokeWidth = 2.5f,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }

                    // 杯中数字（横排）
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "$cups",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cups >= currentGoal) AccentTeal else waterColor
                        )
                        Text(
                            "/$currentGoal",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 状态文字
                Text(
if (cups >= currentGoal) "今日饮水目标已达成！"
                            else "还差 ${currentGoal - cups} 杯就达标啦~",
                    fontSize = 13.sp,
                    color = if (cups >= currentGoal) AccentTeal else TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 操作按钮
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 减少
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (cups > 0) Color(0xFFFFEBEE) else Color(0xFFF5F5F5))
                            .clickable(enabled = cups > 0) { cups-- },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Remove, "减少", Modifier.size(20.dp),
                            tint = if (cups > 0) PeriodRed else TextHint)
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // 喝一杯
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = waterColor,
                        shadowElevation = 4.dp,
                        modifier = Modifier.clickable { cups++ }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.WaterDrop, null, Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("喝一杯", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // 重置
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5))
                            .clickable { cups = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Refresh, "重置", Modifier.size(20.dp), tint = TextHint)
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
    var showSleepTip by remember { mutableStateOf(false) }

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

    // 入睡按钮是否禁用：已记录入睡且还没起床 → 禁用
    val isSleeping = bedtime.isNotEmpty() && waketime.isEmpty()

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
            com.laileme.app.data.SyncManager.triggerImmediateSync()
        }
    }

    // 睡眠质量评估
    val (qualityIcon, qualityText, qualityColor) = when {
        duration == null -> Triple(Icons.Outlined.Bedtime, if (isSleeping) "睡眠中…" else "记录睡眠吧", if (isSleeping) sleepColor else TextHint)
        duration.first < 6 -> Triple(Icons.Outlined.SentimentVeryDissatisfied, "睡眠不足", PeriodRed)
        duration.first < 7 -> Triple(Icons.Outlined.SentimentNeutral, "还可以哦", AccentOrange)
        duration.first < 9 -> Triple(Icons.Outlined.SentimentVerySatisfied, "很棒！", AccentTeal)
        else -> Triple(Icons.Outlined.SentimentDissatisfied, "睡太多啦", AccentOrange)
    }

    // 睡觉提示弹窗
    if (showSleepTip) {
        AlertDialog(
            onDismissRequest = { showSleepTip = false },
            confirmButton = {
                TextButton(onClick = { showSleepTip = false }) {
                    Text("晚安", color = Color(0xFF9C27B0), fontWeight = FontWeight.Bold)
                }
            },
            icon = {
                Icon(
                    Icons.Outlined.NightsStay,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Color(0xFF9C27B0)
                )
            },
            title = { Text("晚安～", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = { Text("该放下手机睡觉了哦～\n祝你做个好梦", fontSize = 15.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 22.sp, modifier = Modifier.fillMaxWidth()) },
            shape = RoundedCornerShape(20.dp)
        )
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
                    Text(
                        if (isSleeping) "正在睡眠中…" else "好好休息，元气满满",
                        fontSize = 11.sp,
                        color = if (isSleeping) sleepColor else TextHint
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 睡眠时间卡片
            Row(modifier = Modifier.fillMaxWidth()) {
                // 入睡按钮
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (isSleeping) Modifier // 已入睡，不可点击
                            else Modifier.clickable {
                                val now = getCurrentTimeStr()
                                bedtime = now
                                waketime = "" // 重置起床时间（新的睡眠周期）
                                saveSleep(now, "")
                                showSleepTip = true
                            }
                        ),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSleeping) Color(0xFFEEEEEE) else Color(0xFFF3E5F5)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.NightsStay, null,
                            Modifier.size(22.dp),
                            tint = if (isSleeping) TextHint else Color(0xFF9C27B0)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("入睡", fontSize = 11.sp, color = if (isSleeping) TextHint else TextSecondary)
                        Text(
                            if (bedtime.isEmpty()) "点击记录" else bedtime,
                            fontSize = if (bedtime.isEmpty()) 13.sp else 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSleeping) TextHint else if (bedtime.isEmpty()) TextHint else Color(0xFF9C27B0)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 起床按钮 — 只有在已入睡时才可点击
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (bedtime.isEmpty()) Modifier // 还没睡，不能起床
                            else Modifier.clickable {
                                val now = getCurrentTimeStr()
                                waketime = now
                                saveSleep(bedtime, now)
                            }
                        ),
                    shape = RoundedCornerShape(14.dp),
                    color = if (bedtime.isEmpty()) Color(0xFFEEEEEE) else Color(0xFFFFF8E1)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.WbSunny, null,
                            Modifier.size(22.dp),
                            tint = if (bedtime.isEmpty()) TextHint else AccentOrange
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("起床", fontSize = 11.sp, color = if (bedtime.isEmpty()) TextHint else TextSecondary)
                        Text(
                            if (waketime.isEmpty()) "点击记录" else waketime,
                            fontSize = if (waketime.isEmpty()) 13.sp else 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bedtime.isEmpty()) TextHint else if (waketime.isEmpty()) TextHint else AccentOrange
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
// 专注模式（番茄钟）— 使用全局 FocusTimerState
// ══════════════════════════════════════════
@Composable
private fun FocusModeSection(prefs: SharedPreferences) {
    val context = LocalContext.current

    // 初始化全局状态
    LaunchedEffect(Unit) {
        com.laileme.app.ui.FocusTimerState.init(context)
    }

    // 从全局状态收集
    val isRunning by com.laileme.app.ui.FocusTimerState.isRunning.collectAsState()
    val isPaused by com.laileme.app.ui.FocusTimerState.isPaused.collectAsState()
    val isBreak by com.laileme.app.ui.FocusTimerState.isBreak.collectAsState()
    val remainingSeconds by com.laileme.app.ui.FocusTimerState.remainingSeconds.collectAsState()
    val completedToday by com.laileme.app.ui.FocusTimerState.completedToday.collectAsState()
    val totalFocusMinutesToday by com.laileme.app.ui.FocusTimerState.totalFocusMinutesToday.collectAsState()
    val focusDuration by com.laileme.app.ui.FocusTimerState.focusDuration.collectAsState()
    val breakDuration by com.laileme.app.ui.FocusTimerState.breakDuration.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    val totalSeconds = if (isBreak) breakDuration * 60 else focusDuration * 60
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 1f


    val focusColor = Color(0xFFFF7043)
    val breakColor = Color(0xFF66BB6A)
    val activeColor = if (isBreak) breakColor else focusColor

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionIcon(Icons.Outlined.Timer, focusColor, Color(0xFFFBE9E7))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("专注模式", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        if (isRunning && !isBreak) "专注中…保持专注！"
                        else if (isRunning && isBreak) "休息一下～"
                        else "番茄钟 · ${focusDuration}分钟专注 + ${breakDuration}分钟休息",
                        fontSize = 11.sp,
                        color = if (isRunning) activeColor else TextHint
                    )
                }
                // 设置按钮
                if (!isRunning) {
                    Icon(
                        Icons.Outlined.Settings, "设置",
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable { showSettingsDialog = true },
                        tint = TextHint
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 圆形计时器 ──
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(300),
                label = "focusProgress"
            )

            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // 背景圆环
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 12f
                    val arcSize = size.width - strokeW * 2
                    val topLeft = androidx.compose.ui.geometry.Offset(strokeW, strokeW)

                    // 底圈
                    drawArc(
                        color = activeColor.copy(alpha = 0.12f),
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
                    // 进度圈
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

                // 中间文字
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 状态标签
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = activeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            if (isBreak) "休息中" else "专注中",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = activeColor
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // 时间显示（点击进入全屏Activity）
                    val min = remainingSeconds / 60
                    val sec = remainingSeconds % 60
                    Text(
                        String.format("%02d:%02d", min, sec),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                context.startActivity(
                                    android.content.Intent(context, com.laileme.app.ui.FocusFullScreenActivity::class.java)
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "第 ${completedToday + 1} 个番茄",
                        fontSize = 12.sp,
                        color = TextHint
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Fullscreen, null,
                            Modifier.size(14.dp),
                            tint = activeColor.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            "点击时间进入全屏专注",
                            fontSize = 10.sp,
                            color = activeColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 控制按钮 ──
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRunning) {
                    // 放弃按钮
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE))
                            .clickable { com.laileme.app.ui.FocusTimerState.stop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Stop, "放弃", Modifier.size(22.dp), tint = PeriodRed)
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // 暂停/继续按钮
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = activeColor,
                        shadowElevation = 4.dp,
                        modifier = Modifier.clickable { com.laileme.app.ui.FocusTimerState.togglePause() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                                null, Modifier.size(20.dp), tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isPaused) "继续" else "暂停",
                                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White
                            )
                        }
                    }
                } else {
                    // 开始按钮
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = focusColor,
                        shadowElevation = 4.dp,
                        modifier = Modifier.clickable { com.laileme.app.ui.FocusTimerState.start() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 36.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.PlayArrow, null, Modifier.size(22.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("开始专注", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 今日统计 ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFF8F0)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 完成番茄数
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.EmojiEvents, null, Modifier.size(20.dp), tint = Color(0xFFFFA726))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$completedToday",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text("完成番茄", fontSize = 11.sp, color = TextHint)
                    }

                    // 分割线
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Color(0xFFE0E0E0))
                    )

                    // 总专注时长
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Schedule, null, Modifier.size(20.dp), tint = Color(0xFFFF7043))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (totalFocusMinutesToday >= 60)
                                "${totalFocusMinutesToday / 60}h${totalFocusMinutesToday % 60}m"
                            else "${totalFocusMinutesToday}min",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text("总专注时长", fontSize = 11.sp, color = TextHint)
                    }

                    // 分割线
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Color(0xFFE0E0E0))
                    )

                    // 鼓励
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Favorite, null, Modifier.size(20.dp), tint = PrimaryPink)
                        Spacer(modifier = Modifier.height(4.dp))
                        val encourage = when {
                            completedToday == 0 -> "加油"
                            completedToday < 4 -> "不错"
                            completedToday < 8 -> "厉害"
                            else -> "超棒"
                        }
                        Text(
                            encourage,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            when {
                                completedToday == 0 -> "开始第一个吧"
                                completedToday < 4 -> "继续保持"
                                completedToday < 8 -> "效率达人"
                                else -> "专注之星"
                            },
                            fontSize = 11.sp, color = TextHint
                        )
                    }
                }
            }

            // 番茄进度点
            if (completedToday > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val displayCount = completedToday.coerceAtMost(12)
                    repeat(displayCount) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(focusColor)
                        )
                    }
                    if (completedToday > 12) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+${completedToday - 12}", fontSize = 11.sp, color = focusColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 提示
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lightbulb, null, Modifier.size(14.dp), tint = TextHint)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "每完成一个番茄钟休息${breakDuration}分钟，保持高效不疲劳",
                    fontSize = 11.sp,
                    color = TextHint,
                    lineHeight = 16.sp
                )
            }
        }
    }


    // ── 设置对话框 ──
    if (showSettingsDialog) {
        var focusInput by remember { mutableStateOf(focusDuration.toString()) }
        var breakInput by remember { mutableStateOf(breakDuration.toString()) }
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = { Text("番茄钟设置", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("自定义专注和休息时间", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = focusInput,
                        onValueChange = { focusInput = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("专注时长（分钟）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        suffix = { Text("分钟") }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = breakInput,
                        onValueChange = { breakInput = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("休息时长（分钟）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        suffix = { Text("分钟") }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 快捷预设
                    Text("快捷预设", fontSize = 12.sp, color = TextHint, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("经典", "25", "5"),
                            Triple("短冲刺", "15", "3"),
                            Triple("深度", "50", "10")
                        ).forEach { (label, f, b) ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        focusInput = f
                                        breakInput = b
                                    },
                                shape = RoundedCornerShape(20.dp),
                                color = if (focusInput == f && breakInput == b) focusColor else Color(0xFFF5F5F5)
                            ) {
                                Text(
                                    "$label ${f}+${b}",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    fontSize = 12.sp,
                                    color = if (focusInput == f && breakInput == b) Color.White else TextSecondary,
                                    fontWeight = if (focusInput == f && breakInput == b) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val f = focusInput.toIntOrNull()
                    val b = breakInput.toIntOrNull()
                    if (f != null && f in 1..120 && b != null && b in 1..30) {
                        com.laileme.app.ui.FocusTimerState.updateSettings(f, b)
                        showSettingsDialog = false
                    }
                }) { Text("确定", color = PrimaryPink) }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }
}

// ══════════════════════════════════════════
// 习惯打卡
// ══════════════════════════════════════════
@Composable
private fun HabitTrackerSection(prefs: SharedPreferences) {
    var myHabits by remember { mutableStateOf(loadHabits(prefs)) }
    var checkins by remember { mutableStateOf(loadCheckins(prefs)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Habit?>(null) }
    var remindedHabits by remember { mutableStateOf(setOf<String>()) } // 已提醒过的习惯id
    var remindSnackbar by remember { mutableStateOf<String?>(null) }

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val habitColor = Color(0xFFAB47BC)
    val habitBg = Color(0xFFF3E5F5)
    val scope = rememberCoroutineScope()

    // 获取token
    val context = LocalContext.current
    val token = remember {
        context.getSharedPreferences("laileme_auth", Context.MODE_PRIVATE)
            .getString("token", "") ?: ""
    }
    val partnerCheckins = remember {
        val raw = prefs.getString("partner_habit_checkins", "") ?: ""
        if (raw.isEmpty()) emptyMap()
        else raw.split(";;").filter { it.contains("::") }.mapNotNull {
            val parts = it.split("::", limit = 2)
            if (parts.size == 2 && parts[1].isNotEmpty()) parts[0] to parts[1].split(",").toSet()
            else null
        }.toMap()
    }

    // 加载伴侣的双人习惯并合并（只合并isCouple=true且本地不存在的）
    val partnerCoupleHabits = remember {
        val raw = prefs.getString("partner_habits", "") ?: ""
        if (raw.isEmpty()) emptyList()
        else raw.split(";;").mapNotNull {
            val parts = it.split("||")
            if (parts.size >= 7 && parts[6] == "true") {
                Habit(parts[0], parts[1], parts[2].toIntOrNull() ?: 0, parts[3].toIntOrNull() ?: 0,
                      parts[4], parts[5], true)
            } else null
        }
    }

    // 合并：我的所有习惯 + 伴侣的双人习惯（去重）
    val myHabitIds = myHabits.map { it.id }.toSet()
    val habits = myHabits + partnerCoupleHabits.filter { it.id !in myHabitIds }

    // 今日需要打卡的习惯
    val todayHabits = habits.filter { shouldCheckToday(it) }
    val checkedCount = todayHabits.count { today in (checkins[it.id] ?: emptySet()) }
    val totalCount = todayHabits.size

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
                SectionIcon(Icons.Outlined.CheckCircle, habitColor, habitBg)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("习惯打卡", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        if (totalCount == 0) "添加你的第一个习惯吧"
                        else if (checkedCount == totalCount) "今日全部完成！太棒了"
                        else "今日 $checkedCount/$totalCount 已完成",
                        fontSize = 11.sp,
                        color = if (checkedCount == totalCount && totalCount > 0) Color(0xFF66BB6A) else TextHint
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(habitColor.copy(alpha = 0.12f))
                        .clickable { showAddDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Add, "添加", Modifier.size(18.dp), tint = habitColor)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (habits.isEmpty()) {
                // 空状态
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFAF0FF)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.AddTask, null,
                            Modifier.size(40.dp), tint = TextHint
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "还没有添加习惯哦~\n点击右上角 + 开始培养好习惯",
                            fontSize = 13.sp, color = TextHint,
                            textAlign = TextAlign.Center, lineHeight = 20.sp
                        )
                    }
                }
            } else {
                // 今日进度条
                if (totalCount > 0) {
                    val progressAnim by animateFloatAsState(
                        targetValue = checkedCount.toFloat() / totalCount.toFloat(),
                        animationSpec = tween(500),
                        label = "habitProgress"
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFAF0FF)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("今日进度", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    "${(progressAnim * 100).toInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (checkedCount == totalCount) Color(0xFF66BB6A) else habitColor
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE0E0E0))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progressAnim)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (checkedCount == totalCount)
                                                Color(0xFF66BB6A)
                                            else habitColor
                                        )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 习惯列表
                habits.forEachIndexed { index, habit ->
                    val isCheckedToday = today in (checkins[habit.id] ?: emptySet())
                    val streak = getStreak(checkins[habit.id])
                    val color = habitColorOptions.getOrElse(habit.colorIndex) { habitColor }
                    val needToday = shouldCheckToday(habit)

                    // 伴侣是否已打卡
                    val partnerChecked = if (habit.isCouple) {
                        today in (partnerCheckins[habit.id] ?: emptySet())
                    } else false

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = needToday && !isCheckedToday) {
                                val newCheckins = checkins.toMutableMap()
                                val dates = (newCheckins[habit.id] ?: emptySet()).toMutableSet()
                                dates.add(today)
                                newCheckins[habit.id] = dates
                                checkins = newCheckins
                                saveCheckins(prefs, checkins)
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCheckedToday) color.copy(alpha = 0.08f) else Color(0xFFFAFAFA)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 图标
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (isCheckedToday) color.copy(alpha = 0.18f) else color.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCheckedToday) {
                                    Icon(
                                        Icons.Outlined.CheckCircle, null,
                                        Modifier.size(22.dp), tint = color
                                    )
                                } else {
                                    val iconPair = habitIconOptions.getOrElse(habit.iconIndex) { Icons.Outlined.Favorite to "打卡" }
                                    Icon(
                                        iconPair.first, null,
                                        Modifier.size(22.dp), tint = color
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        habit.name, fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium, color = TextPrimary,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                    if (habit.isCouple) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFCE4EC)
                                        ) {
                                            Text(
                                                "双人",
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                fontSize = 9.sp, color = PrimaryPink, fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (streak > 0) {
                                        Icon(Icons.Outlined.LocalFireDepartment, null, Modifier.size(14.dp), tint = Color(0xFFFF7043))
                                        Text(
                                            "连续${streak}天",
                                            fontSize = 12.sp, color = Color(0xFFFF7043), fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        if (!needToday) "今日无需打卡"
                                        else if (isCheckedToday) "已完成"
                                        else "点击打卡",
                                        fontSize = 12.sp,
                                        color = if (isCheckedToday) Color(0xFF66BB6A) else TextHint
                                    )
                                }
                                // 伴侣打卡状态
                                if (habit.isCouple) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.FavoriteBorder, null,
                                            Modifier.size(12.dp),
                                            tint = if (partnerChecked) Color(0xFF66BB6A) else TextHint
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            if (partnerChecked) "TA也完成了"
                                            else if (habit.id in remindedHabits) "已提醒"
                                            else "等待TA打卡…",
                                            fontSize = 11.sp,
                                            color = if (partnerChecked) Color(0xFF66BB6A)
                                                    else if (habit.id in remindedHabits) PrimaryPink
                                                    else TextHint
                                        )
                                        // 提醒Ta按钮（伴侣未打卡且未提醒过时显示）
                                        if (!partnerChecked && habit.id !in remindedHabits && token.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        scope.launch {
                                                            try {
                                                                val (ok, _) = com.laileme.app.data.PartnerManager.sendCareMessage(
                                                                    token, "💪 提醒你完成习惯打卡「${habit.name}」～一起加油！"
                                                                )
                                                                if (ok) {
                                                                    remindedHabits = remindedHabits + habit.id
                                                                    remindSnackbar = "已提醒TA打卡「${habit.name}」💕"
                                                                } else {
                                                                    remindSnackbar = "提醒发送失败，请稍后再试"
                                                                }
                                                            } catch (_: Exception) {
                                                                remindSnackbar = "网络错误，请稍后再试"
                                                            }
                                                        }
                                                    },
                                                shape = RoundedCornerShape(8.dp),
                                                color = PrimaryPink.copy(alpha = 0.12f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Notifications, null,
                                                        Modifier.size(11.dp), tint = PrimaryPink
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("提醒Ta", fontSize = 10.sp, color = PrimaryPink, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 今日状态 + 长按删除
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (isCheckedToday) {
                                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(24.dp), tint = Color(0xFF66BB6A))
                                } else if (needToday) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE0E0E0)),
                                        contentAlignment = Alignment.Center
                                    ) {}
                                } else {
                                    Text("—", fontSize = 16.sp, color = TextHint)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                // 删除按钮
                                Icon(
                                    Icons.Outlined.Close, "删除",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .clickable { deleteTarget = habit },
                                    tint = TextHint.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    if (index < habits.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // 取消打卡提示
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lightbulb, null, Modifier.size(14.dp), tint = TextHint)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "坚持打卡，养成好习惯！双人模式可与伴侣互相监督",
                        fontSize = 11.sp, color = TextHint, lineHeight = 16.sp
                    )
                }
            }
        }
    }

    // 提醒结果Toast
    LaunchedEffect(remindSnackbar) {
        if (remindSnackbar != null) {
            android.widget.Toast.makeText(context, remindSnackbar, android.widget.Toast.LENGTH_SHORT).show()
            delay(2000)
            remindSnackbar = null
        }
    }

    // 添加习惯对话框
    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, iconIndex, colorIndex, frequency, frequencyData, isCouple ->
                val id = System.currentTimeMillis().toString()
                myHabits = myHabits + Habit(id, name, iconIndex, colorIndex, frequency, frequencyData, isCouple)
                saveHabits(prefs, myHabits)
                showAddDialog = false
            }
        )
    }

    // 删除确认
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            icon = { Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(32.dp), tint = PeriodRed) },
            title = { Text("删除习惯", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("确定删除「${deleteTarget?.name}」吗？打卡记录也会一起删除哦", fontSize = 14.sp, color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    val target = deleteTarget!!
                    myHabits = myHabits.filter { it.id != target.id }
                    val newCheckins = checkins.toMutableMap()
                    newCheckins.remove(target.id)
                    checkins = newCheckins
                    saveHabits(prefs, myHabits)
                    saveCheckins(prefs, checkins)
                    deleteTarget = null
                }) { Text("删除", color = PeriodRed) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun AddHabitDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Int, Int, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var iconIndex by remember { mutableStateOf(0) }
    var colorIndex by remember { mutableStateOf(0) }
    var frequency by remember { mutableStateOf("daily") }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var isCouple by remember { mutableStateOf(false) }

    val weekDayNames = listOf("一", "二", "三", "四", "五", "六", "日")
    val habitColor = Color(0xFFAB47BC)
    val icons = habitIconOptions

    val canSubmit = name.isNotBlank() && (frequency != "weekly" || selectedDays.isNotEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = { Text("添加新习惯", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // 习惯名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("习惯名称") },
                    placeholder = { Text("如：每天运动30分钟") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 选择图标
                Text("选择图标", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    icons.take(5).forEachIndexed { i, (icon, label) ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (iconIndex == i) habitColorOptions.getOrElse(colorIndex) { habitColor }
                                    else Color(0xFFF5F5F5)
                                )
                                .clickable { iconIndex = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon, label, Modifier.size(20.dp),
                                tint = if (iconIndex == i) Color.White else TextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    icons.drop(5).forEachIndexed { i, (icon, label) ->
                        val idx = i + 5
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (iconIndex == idx) habitColorOptions.getOrElse(colorIndex) { habitColor }
                                    else Color(0xFFF5F5F5)
                                )
                                .clickable { iconIndex = idx },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon, label, Modifier.size(20.dp),
                                tint = if (iconIndex == idx) Color.White else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 选择颜色
                Text("选择颜色", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    habitColorOptions.take(5).forEachIndexed { i, c ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { colorIndex = i },
                            contentAlignment = Alignment.Center
                        ) {
                            if (colorIndex == i) {
                                Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = Color.White)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    habitColorOptions.drop(5).forEachIndexed { i, c ->
                        val idx = i + 5
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { colorIndex = idx },
                            contentAlignment = Alignment.Center
                        ) {
                            if (colorIndex == idx) {
                                Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 频率
                Text("打卡频率", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("daily" to "每天", "weekly" to "自选周几").forEach { (type, label) ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { frequency = type },
                            shape = RoundedCornerShape(20.dp),
                            color = if (frequency == type) habitColor else Color(0xFFF5F5F5)
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 13.sp,
                                color = if (frequency == type) Color.White else TextSecondary,
                                fontWeight = if (frequency == type) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // 每周选择
                if (frequency == "weekly") {
                    Spacer(modifier = Modifier.height(10.dp))
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
                                    .background(if (isSelected) habitColor else Color(0xFFF5F5F5))
                                    .clickable {
                                        selectedDays = if (isSelected) selectedDays - dayNum else selectedDays + dayNum
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    dayName, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                    if (selectedDays.isEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("请至少选择一天～", fontSize = 11.sp, color = habitColor)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 双人模式
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isCouple = !isCouple },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCouple) Color(0xFFFCE4EC) else Color(0xFFF5F5F5)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.FavoriteBorder, null,
                            Modifier.size(20.dp),
                            tint = if (isCouple) PrimaryPink else TextHint
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "双人模式",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isCouple) PrimaryPink else TextPrimary
                            )
                            Text(
                                "与伴侣互相监督，一起打卡",
                                fontSize = 11.sp,
                                color = TextHint
                            )
                        }
                        Switch(
                            checked = isCouple,
                            onCheckedChange = { isCouple = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryPink
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (canSubmit) {
                        val freqData = when (frequency) {
                            "weekly" -> selectedDays.sorted().joinToString(",")
                            else -> ""
                        }
                        onAdd(name.trim(), iconIndex, colorIndex, frequency, freqData, isCouple)
                    }
                },
                enabled = canSubmit
            ) { Text("添加", color = if (canSubmit) habitColor else TextHint) }
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

