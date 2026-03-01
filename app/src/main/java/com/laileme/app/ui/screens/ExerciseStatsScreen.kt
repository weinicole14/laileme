package com.laileme.app.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laileme.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ══════════════════════════════════════════
// 运动统计详情页
// ══════════════════════════════════════════

// 数据类放在 Composable 外面，避免每次重组重建
private data class DayData(val dateKey: String, val steps: Int, val calories: Int, val exercise: Int)
private data class TotalStats(val steps: Long, val calories: Long, val exercise: Long, val days: Int)

@Composable
fun ExerciseStatsScreen(
    onBack: () -> Unit,
    currentUserId: String
) {
    val context = LocalContext.current
    val healthPrefs = remember {
        context.getSharedPreferences("laileme_health_$currentUserId", Context.MODE_PRIVATE)
    }

    BackHandler { onBack() }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // 最近7天日期
    val last7Days = remember {
        (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            sdf.format(cal.time)
        }
    }

    val weekData = remember {
        last7Days.map { dateStr ->
            DayData(
                dateKey = dateStr,
                steps = healthPrefs.getInt("steps_$dateStr", 0),
                calories = healthPrefs.getInt("calories_$dateStr", 0),
                exercise = healthPrefs.getInt("exercise_$dateStr", 0)
            )
        }
    }

    val todayData = weekData.last()
    val yesterdayData = weekData[weekData.size - 2]

    // 总数据：只读一次 all，避免多次磁盘IO
    val totalStats = remember {
        val allEntries = healthPrefs.all
        var steps = 0L; var calories = 0L; var exercise = 0L; var days = 0
        allEntries.forEach { (key, value) ->
            if (value is Int) {
                when {
                    key.startsWith("steps_") -> { steps += value; if (value > 0) days++ }
                    key.startsWith("calories_") -> calories += value
                    key.startsWith("exercise_") -> exercise += value
                }
            }
        }
        TotalStats(steps, calories, exercise, days.coerceAtLeast(1))
    }
    val totalSteps = totalStats.steps
    val totalCalories = totalStats.calories
    val totalExercise = totalStats.exercise
    val totalDays = totalStats.days

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
            .padding(bottom = 80.dp)
    ) {
        // 顶部导航
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Outlined.ArrowBackIosNew,
                    contentDescription = "返回",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "运动统计",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ═══ 7天步数柱状图 ═══
        StepsBarChart(weekData.map { it.dateKey to it.steps })

        Spacer(modifier = Modifier.height(16.dp))

        // ═══ 7天卡路里柱状图 ═══
        CaloriesBarChart(weekData.map { it.dateKey to it.calories })

        Spacer(modifier = Modifier.height(16.dp))

        // ═══ 昨日 / 今日 / 总数据 三张卡片 ═══
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 昨日
            DaySummaryCard(
                title = "昨日",
                steps = yesterdayData.steps,
                calories = yesterdayData.calories,
                exercise = yesterdayData.exercise,
                color = Color(0xFF78909C),
                modifier = Modifier.weight(1f)
            )
            // 今日
            DaySummaryCard(
                title = "今日",
                steps = todayData.steps,
                calories = todayData.calories,
                exercise = todayData.exercise,
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ═══ 总数据卡片（含距离类比） ═══
        TotalDataCard(
            totalSteps = totalSteps,
            totalCalories = totalCalories,
            totalExercise = totalExercise,
            totalDays = totalDays
        )
    }
}

// ══════════════════════════════════════════
// 7天步数柱状图
// ══════════════════════════════════════════
@Composable
private fun StepsBarChart(data: List<Pair<String, Int>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.DirectionsWalk,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "步数（近7天）",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 图例
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 10.dp)
                        .background(Color(0xFFFF9800), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("步数", fontSize = 10.sp, color = TextSecondary)
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 1.dp)
                        .background(Color(0xFF4CAF50))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("目标7500步", fontSize = 10.sp, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 摘要
            val validData = data.filter { it.second > 0 }
            if (validData.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStatItem("日均", "${validData.map { it.second }.average().toInt()}", "步", Color(0xFFFF9800))
                    MiniStatItem("最高", "${validData.maxOf { it.second }}", "步", Color(0xFFE91E63))
                    MiniStatItem("最低", "${validData.minOf { it.second }}", "步", AccentOrange)
                }
            }

            // 柱状图
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val width = size.width
                val height = size.height
                val paddingLeft = 60f
                val paddingRight = 20f
                val paddingTop = 10f
                val paddingBottom = 45f
                val chartHeight = height - paddingTop - paddingBottom
                val chartWidth = width - paddingLeft - paddingRight

                val maxVal = (data.maxOfOrNull { it.second }?.coerceAtLeast(7500) ?: 7500).let {
                    ((it / 2500) + 1) * 2500
                }.toFloat()
                val barCount = data.size
                val barSpacing = chartWidth / barCount
                val barWidth = barSpacing * 0.55f

                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#999999")
                    textSize = 22f
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }

                // Y轴刻度
                val tickStep = if (maxVal > 15000) 5000 else 2500
                var tick = 0
                while (tick <= maxVal) {
                    val ty = height - paddingBottom - (tick / maxVal) * chartHeight
                    drawLine(
                        color = Color(0xFFEEEEEE),
                        start = Offset(paddingLeft, ty),
                        end = Offset(width - paddingRight, ty),
                        strokeWidth = 1f
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        if (tick >= 10000) "${tick / 1000}k" else "$tick",
                        paddingLeft - 10f,
                        ty + 8f,
                        textPaint
                    )
                    tick += tickStep
                }

                // 目标线 7500
                val goalY = height - paddingBottom - (7500f / maxVal) * chartHeight
                val dashLen = 10f
                val gapLen = 6f
                var dx = paddingLeft
                while (dx < width - paddingRight) {
                    val endX = (dx + dashLen).coerceAtMost(width - paddingRight)
                    drawLine(
                        color = Color(0xFF4CAF50).copy(alpha = 0.6f),
                        start = Offset(dx, goalY),
                        end = Offset(endX, goalY),
                        strokeWidth = 2f
                    )
                    dx += dashLen + gapLen
                }

                // 柱状图
                val bottomPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#888888")
                    textSize = 20f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val valuePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#FF9800")
                    textSize = 20f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                }

                data.forEachIndexed { index, (dateStr, steps) ->
                    val centerX = paddingLeft + barSpacing * index + barSpacing / 2
                    val dateLabel = dateStr.takeLast(5) // MM-dd

                    if (steps > 0) {
                        val barH = (steps.toFloat().coerceIn(0f, maxVal) / maxVal) * chartHeight
                        val barTop = height - paddingBottom - barH
                        val barLeft = centerX - barWidth / 2

                        val barColor = when {
                            steps >= 7500 -> Color(0xFF4CAF50) // 达标绿色
                            steps >= 5000 -> Color(0xFFFF9800) // 接近橙色
                            else -> Color(0xFFFF7043) // 偏低偏红
                        }

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        // 数值
                        drawContext.canvas.nativeCanvas.drawText(
                            if (steps >= 10000) "${steps / 1000}k" else "$steps",
                            centerX,
                            barTop - 8f,
                            valuePaint
                        )
                    }

                    // 底部日期
                    drawContext.canvas.nativeCanvas.drawText(
                        dateLabel,
                        centerX,
                        height - paddingBottom + 30f,
                        bottomPaint
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════
// 7天卡路里柱状图
// ══════════════════════════════════════════
@Composable
private fun CaloriesBarChart(data: List<Pair<String, Int>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "消耗（近7天）",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 10.dp)
                        .background(Color(0xFFFFC107), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("消耗(千卡)", fontSize = 10.sp, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(6.dp))

            val validData = data.filter { it.second > 0 }
            if (validData.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStatItem("日均", "${validData.map { it.second }.average().toInt()}", "千卡", Color(0xFFFFC107))
                    MiniStatItem("最高", "${validData.maxOf { it.second }}", "千卡", Color(0xFFE91E63))
                    MiniStatItem("总计", "${validData.sumOf { it.second }}", "千卡", AccentOrange)
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val width = size.width
                val height = size.height
                val paddingLeft = 55f
                val paddingRight = 20f
                val paddingTop = 10f
                val paddingBottom = 45f
                val chartHeight = height - paddingTop - paddingBottom
                val chartWidth = width - paddingLeft - paddingRight

                val maxVal = (data.maxOfOrNull { it.second }?.coerceAtLeast(500) ?: 500).let {
                    ((it / 200) + 1) * 200
                }.toFloat()
                val barCount = data.size
                val barSpacing = chartWidth / barCount
                val barWidth = barSpacing * 0.55f

                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#999999")
                    textSize = 22f
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }

                val tickStep = if (maxVal > 1000) 500 else 200
                var tick = 0
                while (tick <= maxVal) {
                    val ty = height - paddingBottom - (tick / maxVal) * chartHeight
                    drawLine(
                        color = Color(0xFFEEEEEE),
                        start = Offset(paddingLeft, ty),
                        end = Offset(width - paddingRight, ty),
                        strokeWidth = 1f
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "$tick",
                        paddingLeft - 10f,
                        ty + 8f,
                        textPaint
                    )
                    tick += tickStep
                }

                val bottomPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#888888")
                    textSize = 20f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val valuePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#FFC107")
                    textSize = 20f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                }

                data.forEachIndexed { index, (dateStr, cal) ->
                    val centerX = paddingLeft + barSpacing * index + barSpacing / 2
                    val dateLabel = dateStr.takeLast(5)

                    if (cal > 0) {
                        val barH = (cal.toFloat().coerceIn(0f, maxVal) / maxVal) * chartHeight
                        val barTop = height - paddingBottom - barH
                        val barLeft = centerX - barWidth / 2

                        drawRoundRect(
                            color = Color(0xFFFFC107),
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        drawContext.canvas.nativeCanvas.drawText(
                            "$cal",
                            centerX,
                            barTop - 8f,
                            valuePaint
                        )
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        dateLabel,
                        centerX,
                        height - paddingBottom + 30f,
                        bottomPaint
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════
// 迷你统计项
// ══════════════════════════════════════════
@Composable
private fun MiniStatItem(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = TextSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.width(2.dp))
            Text(unit, fontSize = 9.sp, color = TextHint, modifier = Modifier.offset(y = (-2).dp))
        }
    }
}

// ══════════════════════════════════════════
// 昨日/今日数据卡片
// ══════════════════════════════════════════
@Composable
private fun DaySummaryCard(
    title: String,
    steps: Int,
    calories: Int,
    exercise: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 步数
            Row(verticalAlignment = Alignment.Bottom) {
                Icon(
                    Icons.Outlined.DirectionsWalk,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "$steps",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("步", fontSize = 10.sp, color = TextHint, modifier = Modifier.offset(y = (-3).dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 距离
            val distanceKm = steps * 0.7 / 1000.0
            Text(
                text = "≈ ${String.format("%.2f", distanceKm)} km",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 消耗
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("$calories 千卡", fontSize = 12.sp, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 运动时长
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = Color(0xFF42A5F5),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("$exercise 分钟", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

// ══════════════════════════════════════════
// 总数据卡片（含距离 + 能量类比）
// ══════════════════════════════════════════
@Composable
private fun TotalDataCard(
    totalSteps: Long,
    totalCalories: Long,
    totalExercise: Long,
    totalDays: Int
) {
    val totalDistanceKm = remember(totalSteps) { totalSteps * 0.7 / 1000.0 }
    val distAnalogy = remember(totalDistanceKm) { getDistanceAnalogy(totalDistanceKm) }
    val calAnalogy = remember(totalCalories) { getCaloriesAnalogy(totalCalories) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "累计数据",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 总步数
            TotalStatRow(Icons.Outlined.DirectionsWalk, "总步数", formatNumber(totalSteps), "步", Color(0xFFFF9800))
            Spacer(modifier = Modifier.height(8.dp))
            // 总距离
            TotalStatRow(Icons.Outlined.Explore, "总距离", String.format("%.2f", totalDistanceKm), "公里", Color(0xFF4CAF50))
            Spacer(modifier = Modifier.height(8.dp))
            // 总消耗
            TotalStatRow(Icons.Outlined.LocalFireDepartment, "总消耗", formatNumber(totalCalories), "千卡", Color(0xFFFFC107))
            Spacer(modifier = Modifier.height(8.dp))
            // 总运动时长
            val hours = totalExercise / 60
            val mins = totalExercise % 60
            TotalStatRow(
                Icons.Outlined.FitnessCenter, "总运动",
                if (hours > 0) "${hours}h ${mins}m" else "$mins",
                if (hours > 0) "" else "分钟",
                Color(0xFF42A5F5)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // 记录天数
            TotalStatRow(Icons.Outlined.CalendarMonth, "记录", "$totalDays", "天", Color(0xFF9C27B0))

            Spacer(modifier = Modifier.height(20.dp))

            // ═══ 距离探索 ═══
            Divider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(Icons.Outlined.Explore, "距离探索", Color(0xFF26A69A))
            Spacer(modifier = Modifier.height(12.dp))

            // 当前成就
            AchievementBanner(
                icon = Icons.Outlined.EmojiEvents,
                text = distAnalogy.current,
                subtitle = "总距离 ${String.format("%.2f", totalDistanceKm)} 公里",
                bgColor = Color(0xFFFFF8E1),
                iconColor = Color(0xFFFFB300),
                textColor = Color(0xFF5D4037),
                subColor = Color(0xFF8D6E63)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("里程碑", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            distAnalogy.milestones.forEach { m ->
                MilestoneItem(
                    icon = m.icon,
                    iconColor = m.iconColor,
                    label = m.label,
                    detail = "${String.format("%.1f", m.value)} km · ${formatNumber((m.value * 1000 / 0.7).toLong())} 步",
                    achieved = totalDistanceKm >= m.value,
                    progress = (totalDistanceKm / m.value).coerceIn(0.0, 1.0).toFloat(),
                    progressColor = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ═══ 能量探索 ═══
            Divider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(Icons.Outlined.LocalFireDepartment, "能量探索", Color(0xFFFF9800))
            Spacer(modifier = Modifier.height(12.dp))

            AchievementBanner(
                icon = Icons.Outlined.LocalFireDepartment,
                text = calAnalogy.current,
                subtitle = "总消耗 ${formatNumber(totalCalories)} 千卡",
                bgColor = Color(0xFFFFF3E0),
                iconColor = Color(0xFFFF9800),
                textColor = Color(0xFFBF360C),
                subColor = Color(0xFFE65100)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("能量里程碑", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            calAnalogy.milestones.forEach { m ->
                MilestoneItem(
                    icon = m.icon,
                    iconColor = m.iconColor,
                    label = m.label,
                    detail = "${formatNumber(m.value.toLong())} 千卡",
                    achieved = totalCalories >= m.value.toLong(),
                    progress = (totalCalories.toDouble() / m.value).coerceIn(0.0, 1.0).toFloat(),
                    progressColor = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

// ══════════════════════════════════════════
// 复用组件
// ══════════════════════════════════════════
@Composable
private fun TotalStatRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, unit: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.width(48.dp))
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = color)
        if (unit.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(unit, fontSize = 11.sp, color = TextHint, modifier = Modifier.offset(y = (-3).dp))
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
private fun AchievementBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    subtitle: String,
    bgColor: Color,
    iconColor: Color,
    textColor: Color,
    subColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 11.sp, color = subColor)
            }
        }
    }
}

// ══════════════════════════════════════════
// 里程碑项（使用 Icon）
// ══════════════════════════════════════════
@Composable
private fun MilestoneItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    detail: String,
    achieved: Boolean,
    progress: Float,
    progressColor: Color = Color(0xFFFF9800)
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achieved) Color(0xFFE8F5E9) else Color(0xFFFAFAFA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (achieved) Color(0xFFC8E6C9) else iconColor.copy(alpha = 0.12f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = if (achieved) Color(0xFF2E7D32) else iconColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (achieved) Color(0xFF2E7D32) else TextPrimary
                    )
                    Text(text = detail, fontSize = 10.sp, color = TextHint)
                }
                if (achieved) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }
            }
            if (!achieved) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = progressColor,
                    trackColor = Color(0xFFEEEEEE),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

// ══════════════════════════════════════════
// 类比数据结构
// ══════════════════════════════════════════
private class AnalogyResult(
    val current: String,
    val milestones: List<AnalogyMilestone>
)

// 不用 data class，避免 ImageVector 的 equals/hashCode 导致 Compose 重组卡死
private class AnalogyMilestone(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconColor: Color,
    val label: String,
    val value: Double // km 或 kcal
)

/**
 * 距离类比 — 基于成年人平均步幅0.7米
 */
private fun getDistanceAnalogy(totalDistanceKm: Double): AnalogyResult {
    val allMilestones = listOf(
        AnalogyMilestone(Icons.Outlined.Stadium, Color(0xFF78909C), "绕标准操场1圈（400m）", 0.4),
        AnalogyMilestone(Icons.Outlined.School, Color(0xFF5C6BC0), "学校到地铁站（1公里）", 1.0),
        AnalogyMilestone(Icons.Outlined.Store, Color(0xFF26A69A), "逛完一条商业街（2公里）", 2.0),
        AnalogyMilestone(Icons.Outlined.Park, Color(0xFF66BB6A), "绕公园跑一圈（3公里）", 3.0),
        AnalogyMilestone(Icons.Outlined.DirectionsRun, Color(0xFFEF5350), "完成一次5公里跑", 5.0),
        AnalogyMilestone(Icons.Outlined.Straighten, Color(0xFF8D6E63), "走完一条长江大桥（6.7km）", 6.7),
        AnalogyMilestone(Icons.Outlined.GpsFixed, Color(0xFFAB47BC), "10公里挑战赛", 10.0),
        AnalogyMilestone(Icons.Outlined.Water, Color(0xFF29B6F6), "西湖一圈（15km）", 15.0),
        AnalogyMilestone(Icons.Outlined.EmojiEvents, Color(0xFFFFA726), "半程马拉松（21.1km）", 21.1),
        AnalogyMilestone(Icons.Outlined.MilitaryTech, Color(0xFFFFD54F), "全程马拉松（42.2km）", 42.2),
        AnalogyMilestone(Icons.Outlined.Train, Color(0xFF42A5F5), "上海到苏州（100km）", 100.0),
        AnalogyMilestone(Icons.Outlined.Landscape, Color(0xFF8D6E63), "北京到天津（137km）", 137.0),
        AnalogyMilestone(Icons.Outlined.FilterHdr, Color(0xFF66BB6A), "上海到杭州（175km）", 175.0),
        AnalogyMilestone(Icons.Outlined.LocationCity, Color(0xFFFF7043), "成都到重庆（269km）", 269.0),
        AnalogyMilestone(Icons.Outlined.Terrain, Color(0xFF78909C), "南京到上海（300km）", 300.0),
        AnalogyMilestone(Icons.Outlined.Map, Color(0xFF5C6BC0), "北京到济南（406km）", 406.0),
        AnalogyMilestone(Icons.Outlined.Flight, Color(0xFF26A69A), "北京到大连（620km）", 620.0),
        AnalogyMilestone(Icons.Outlined.NearMe, Color(0xFFAB47BC), "北京到南京（1023km）", 1023.0),
        AnalogyMilestone(Icons.Outlined.Apartment, Color(0xFFEF5350), "北京到上海（1213km）", 1213.0),
        AnalogyMilestone(Icons.Outlined.AccountBalance, Color(0xFF8D6E63), "北京到西安（1076km）", 1076.0),
        AnalogyMilestone(Icons.Outlined.BeachAccess, Color(0xFF66BB6A), "北京到广州（1888km）", 1888.0),
        AnalogyMilestone(Icons.Outlined.Forest, Color(0xFF2E7D32), "北京到成都（1808km）", 1808.0),
        AnalogyMilestone(Icons.Outlined.Hiking, Color(0xFF5D4037), "北京到拉萨（3650km）", 3650.0),
        AnalogyMilestone(Icons.Outlined.TravelExplore, Color(0xFFF4511E), "丝绸之路·长安到敦煌（1900km）", 1900.0),
        AnalogyMilestone(Icons.Outlined.Flag, Color(0xFFD32F2F), "中国南北跨度（5500km）", 5500.0),
        AnalogyMilestone(Icons.Outlined.Castle, Color(0xFF6D4C41), "丝绸之路全程·长安到罗马（7000km）", 7000.0),
        AnalogyMilestone(Icons.Outlined.Fence, Color(0xFF795548), "万里长城全长（21196km）", 21196.0),
        AnalogyMilestone(Icons.Outlined.Public, Color(0xFF2196F3), "赤道一圈（40075km）", 40075.0),
        AnalogyMilestone(Icons.Outlined.NightlightRound, Color(0xFF9E9E9E), "地球到月球（384400km）", 384400.0)
    )

    val current = when {
        totalDistanceKm < 0.4 -> "刚刚起步，每一步都算数！"
        totalDistanceKm < 1.0 -> "已经走了${(totalDistanceKm / 0.4).toInt()}圈操场！"
        totalDistanceKm < 5.0 -> "已走 ${String.format("%.1f", totalDistanceKm)} 公里，相当于绕操场 ${(totalDistanceKm / 0.4).toInt()} 圈！"
        totalDistanceKm < 10.0 -> "累计 ${String.format("%.1f", totalDistanceKm)} 公里，相当于 ${String.format("%.1f", totalDistanceKm / 5.0)} 次5K跑！"
        totalDistanceKm < 42.2 -> "累计 ${String.format("%.1f", totalDistanceKm)} 公里，完成了 ${String.format("%.1f", totalDistanceKm / 21.1)} 次半马！"
        totalDistanceKm < 100.0 -> "累计 ${String.format("%.1f", totalDistanceKm)} 公里，相当于 ${String.format("%.1f", totalDistanceKm / 42.2)} 个全程马拉松！"
        totalDistanceKm < 500.0 -> "累计 ${String.format("%.0f", totalDistanceKm)} 公里，从上海走到苏州 ${String.format("%.1f", totalDistanceKm / 100.0)} 趟！"
        totalDistanceKm < 1213.0 -> "累计 ${String.format("%.0f", totalDistanceKm)} 公里，走了北京到上海 ${(totalDistanceKm / 1213.0 * 100).toInt()}%！"
        totalDistanceKm < 3650.0 -> "累计 ${String.format("%.0f", totalDistanceKm)} 公里，从北京走到上海 ${String.format("%.1f", totalDistanceKm / 1213.0)} 次！"
        totalDistanceKm < 7000.0 -> "累计 ${String.format("%.0f", totalDistanceKm)} 公里，从北京走到拉萨 ${String.format("%.1f", totalDistanceKm / 3650.0)} 趟！"
        totalDistanceKm < 21196.0 -> "累计 ${String.format("%.0f", totalDistanceKm)} 公里，重走丝绸之路 ${(totalDistanceKm / 7000.0 * 100).toInt()}%！"
        totalDistanceKm < 40075.0 -> "累计 ${String.format("%.0f", totalDistanceKm)} 公里，走完长城 ${(totalDistanceKm / 21196.0 * 100).toInt()}%！"
        totalDistanceKm < 384400.0 -> "累计 ${String.format("%.0f", totalDistanceKm)} 公里，绕地球 ${String.format("%.2f", totalDistanceKm / 40075.0)} 圈！"
        else -> "累计 ${String.format("%.0f", totalDistanceKm)} 公里，到月球 ${String.format("%.1f", totalDistanceKm / 384400.0)} 趟！"
    }

    val achievedCount = allMilestones.count { totalDistanceKm >= it.value }
    val startIdx = (achievedCount - 2).coerceAtLeast(0)
    val endIdx = (achievedCount + 4).coerceAtMost(allMilestones.size)
    return AnalogyResult(current, allMilestones.subList(startIdx, endIdx))
}

/**
 * 能量类比 — 基于常见食物/活动的热量换算
 * 1千卡 ≈ 1大卡 ≈ 4.184千焦
 */
private fun getCaloriesAnalogy(totalCalories: Long): AnalogyResult {
    val allMilestones = listOf(
        // 食物类比（1根香蕉≈89kcal, 1碗米饭≈232kcal, 1个汉堡≈295kcal等）
        AnalogyMilestone(Icons.Outlined.LocalCafe, Color(0xFF8D6E63), "1杯拿铁咖啡（150千卡）", 150.0),
        AnalogyMilestone(Icons.Outlined.RiceBowl, Color(0xFFFF9800), "1碗米饭（232千卡）", 232.0),
        AnalogyMilestone(Icons.Outlined.Fastfood, Color(0xFFE53935), "1个牛肉汉堡（295千卡）", 295.0),
        AnalogyMilestone(Icons.Outlined.LunchDining, Color(0xFFF4511E), "1份炸鸡套餐（500千卡）", 500.0),
        AnalogyMilestone(Icons.Outlined.DinnerDining, Color(0xFF6D4C41), "1顿正餐（800千卡）", 800.0),
        AnalogyMilestone(Icons.Outlined.LocalPizza, Color(0xFFFF7043), "1整个9寸披萨（2100千卡）", 2100.0),
        AnalogyMilestone(Icons.Outlined.Cake, Color(0xFFEC407A), "1个8寸生日蛋糕（3500千卡）", 3500.0),
        AnalogyMilestone(Icons.Outlined.Restaurant, Color(0xFF26A69A), "一天三餐总热量（2000千卡）", 2000.0),
        AnalogyMilestone(Icons.Outlined.LocalFireDepartment, Color(0xFFFF6F00), "燃烧1斤脂肪（3850千卡）", 3850.0),
        AnalogyMilestone(Icons.Outlined.FitnessCenter, Color(0xFF42A5F5), "1周三餐（14000千卡）", 14000.0),
        AnalogyMilestone(Icons.Outlined.MonitorWeight, Color(0xFFAB47BC), "减掉1公斤脂肪（7700千卡）", 7700.0),
        AnalogyMilestone(Icons.Outlined.ShoppingCart, Color(0xFF66BB6A), "一个月的餐食（60000千卡）", 60000.0),
        AnalogyMilestone(Icons.Outlined.Spa, Color(0xFF29B6F6), "减掉10斤（38500千卡）", 38500.0),
        AnalogyMilestone(Icons.Outlined.Pool, Color(0xFF0097A7), "游泳100小时（70000千卡）", 70000.0),
        AnalogyMilestone(Icons.Outlined.DirectionsBike, Color(0xFF689F38), "骑行环海南岛（30000千卡）", 30000.0),
        AnalogyMilestone(Icons.Outlined.Whatshot, Color(0xFFFF3D00), "烧开100壶水（100000千卡）", 100000.0),
        AnalogyMilestone(Icons.Outlined.BatteryChargingFull, Color(0xFF4CAF50), "手机充电50000次（200000千卡）", 200000.0),
        AnalogyMilestone(Icons.Outlined.ElectricBolt, Color(0xFFFFC107), "1度电 = 860千卡 × 1000（860000千卡）", 860000.0),
        AnalogyMilestone(Icons.Outlined.RocketLaunch, Color(0xFFE91E63), "一辆汽车行驶100km油耗（2000000千卡）", 2000000.0)
    )

    val current = when {
        totalCalories < 150 -> "刚开始燃烧，继续加油！"
        totalCalories < 500 -> "已消耗 $totalCalories 千卡，≈ ${totalCalories / 89}根香蕉的能量！"
        totalCalories < 2000 -> "已消耗 $totalCalories 千卡，≈ ${totalCalories / 232}碗米饭！"
        totalCalories < 7700 -> "已消耗 ${formatNumber(totalCalories)} 千卡，≈ ${totalCalories / 2000}天的三餐热量！"
        totalCalories < 38500 -> "已消耗 ${formatNumber(totalCalories)} 千卡，≈ 燃烧 ${String.format("%.1f", totalCalories / 7700.0)} 公斤脂肪！"
        totalCalories < 100000 -> "已消耗 ${formatNumber(totalCalories)} 千卡，≈ 减掉 ${String.format("%.1f", totalCalories / 3850.0)} 斤！"
        totalCalories < 500000 -> "已消耗 ${formatNumber(totalCalories)} 千卡，≈ ${totalCalories / 60000}个月的餐食！"
        totalCalories < 2000000 -> "已消耗 ${formatNumber(totalCalories)} 千卡，≈ 烧开 ${totalCalories / 1000}壶水！"
        else -> "已消耗 ${formatNumber(totalCalories)} 千卡，能量惊人！足以让汽车跑 ${String.format("%.0f", totalCalories / 20000.0)} 公里！"
    }

    val achievedCount = allMilestones.count { totalCalories >= it.value.toLong() }
    val startIdx = (achievedCount - 2).coerceAtLeast(0)
    val endIdx = (achievedCount + 4).coerceAtMost(allMilestones.size)
    return AnalogyResult(current, allMilestones.subList(startIdx, endIdx))
}

// 格式化数字（加千分位分隔符）
private fun formatNumber(n: Long): String {
    if (n < 1000) return "$n"
    return String.format("%,d", n)
}
