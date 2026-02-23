package com.laileme.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laileme.app.data.AppDatabase
import com.laileme.app.data.entity.PeriodRecord
import com.laileme.app.data.entity.SleepRecord
import com.laileme.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen(records: List<PeriodRecord>) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val sleepRecords by db.sleepDao().getRecent(7).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
            .padding(bottom = 60.dp)
    ) {
        Text(
            text = "周期统计",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 统计卡片
        StatsCard(records)

        Spacer(modifier = Modifier.height(16.dp))

        // 周期图表
        CycleChart(records)

        Spacer(modifier = Modifier.height(16.dp))

        // 睡眠时长条形图
        SleepBarChart(sleepRecords)

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun StatsCard(records: List<PeriodRecord>) {
    val completedRecords = records.filter { it.endDate != null }
    val avgCycle = if (completedRecords.isNotEmpty()) {
        completedRecords.map { it.cycleLength }.average().toInt()
    } else -1

    val avgPeriod = if (completedRecords.isNotEmpty()) {
        completedRecords.map { it.periodLength }.average().toInt()
    } else -1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("平均周期", if (avgCycle > 0) "${avgCycle}天" else "--", PrimaryPink)
            StatItem("平均经期", if (avgPeriod > 0) "${avgPeriod}天" else "--", AccentTeal)
            StatItem("记录次数", if (completedRecords.isNotEmpty()) "${completedRecords.size}次" else "--", AccentOrange)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun CycleChart(records: List<PeriodRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "周期趋势",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ───── 图例说明 ─────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = PrimaryPink, label = "周期长度")
                LegendItem(color = AccentTeal, label = "经期天数")
                LegendItem(color = AccentOrange, label = "参考线(28天)", isDashed = true)
            }

            Spacer(modifier = Modifier.height(12.dp))

            val completedRecords = records.filter { it.endDate != null }

            if (completedRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无数据，记录经期后查看趋势", color = TextHint, fontSize = 12.sp)
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val paddingLeft = 55f
                    val paddingRight = 20f
                    val paddingTop = 20f
                    val paddingBottom = 30f
                    val chartHeight = height - paddingTop - paddingBottom
                    val chartWidth = width - paddingLeft - paddingRight

                    // Y轴范围 0-45天
                    val minVal = 0f
                    val maxVal = 45f
                    val valRange = maxVal - minVal

                    // ── 参考线：28天理想周期（橙色虚线效果） ──
                    val idealY = height - paddingBottom - ((28f - minVal) / valRange) * chartHeight
                    val dashLen = 12f
                    val gapLen = 8f
                    var drawX = paddingLeft
                    while (drawX < width - paddingRight) {
                        val endX = (drawX + dashLen).coerceAtMost(width - paddingRight)
                        drawLine(
                            color = AccentOrange.copy(alpha = 0.5f),
                            start = Offset(drawX, idealY),
                            end = Offset(endX, idealY),
                            strokeWidth = 2f
                        )
                        drawX += dashLen + gapLen
                    }

                    // ── 横向参考线：5天、10天（浅灰虚线，辅助经期天数阅读）──
                    for (refDay in listOf(5, 10)) {
                        val refY = height - paddingBottom - ((refDay.toFloat() - minVal) / valRange) * chartHeight
                        var rx = paddingLeft
                        while (rx < width - paddingRight) {
                            val ex = (rx + 6f).coerceAtMost(width - paddingRight)
                            drawLine(
                                color = Color(0xFFE0E0E0),
                                start = Offset(rx, refY),
                                end = Offset(ex, refY),
                                strokeWidth = 1f
                            )
                            rx += 12f
                        }
                    }

                    // ── 绘制数据（取最新6条已完成记录，按时间正序排列）──
                    val displayRecords = completedRecords.take(6).reversed()
                    val count = displayRecords.size

                    // 周期长度数据点（粉色）
                    val cyclePoints = displayRecords.mapIndexed { index, record ->
                        val x = paddingLeft + chartWidth * index / maxOf(count - 1, 1)
                        val y = height - paddingBottom - ((record.cycleLength.toFloat().coerceIn(minVal, maxVal) - minVal) / valRange) * chartHeight
                        Offset(x, y)
                    }

                    // 经期天数数据点（蓝绿色）
                    val periodPoints = displayRecords.mapIndexed { index, record ->
                        val x = paddingLeft + chartWidth * index / maxOf(count - 1, 1)
                        val y = height - paddingBottom - ((record.periodLength.toFloat().coerceIn(minVal, maxVal) - minVal) / valRange) * chartHeight
                        Offset(x, y)
                    }

                    // 画周期长度折线
                    if (cyclePoints.size > 1) {
                        val path = Path().apply {
                            moveTo(cyclePoints.first().x, cyclePoints.first().y)
                            cyclePoints.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, PrimaryPink, style = Stroke(width = 3f))
                    }
                    cyclePoints.forEach { point ->
                        drawCircle(PrimaryPink, 8f, point)
                        drawCircle(Color.White, 4f, point)
                    }

                    // 画经期天数折线
                    if (periodPoints.size > 1) {
                        val path = Path().apply {
                            moveTo(periodPoints.first().x, periodPoints.first().y)
                            periodPoints.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, AccentTeal, style = Stroke(width = 3f))
                    }
                    periodPoints.forEach { point ->
                        drawCircle(AccentTeal, 6f, point)
                        drawCircle(Color.White, 3f, point)
                    }

                    // ── Y轴刻度标签 ──
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#999999")
                        textSize = 26f
                        textAlign = android.graphics.Paint.Align.RIGHT
                        isAntiAlias = true
                    }
                    for (tick in listOf(5, 10, 20, 28, 35)) {
                        val ty = height - paddingBottom - ((tick.toFloat() - minVal) / valRange) * chartHeight
                        drawLine(
                            color = Color(0xFFCCCCCC),
                            start = Offset(paddingLeft - 8f, ty),
                            end = Offset(paddingLeft, ty),
                            strokeWidth = 1.5f
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "$tick",
                            paddingLeft - 14f,
                            ty + 9f,
                            textPaint
                        )
                    }
                }

            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, isDashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isDashed) {
            // 虚线样式图例
            Canvas(modifier = Modifier.size(width = 20.dp, height = 10.dp)) {
                var x = 0f
                while (x < size.width) {
                    val end = (x + 6f).coerceAtMost(size.width)
                    drawLine(
                        color = color,
                        start = Offset(x, size.height / 2),
                        end = Offset(end, size.height / 2),
                        strokeWidth = 2f
                    )
                    x += 10f
                }
            }
        } else {
            // 实线 + 圆点样式图例
            Canvas(modifier = Modifier.size(width = 20.dp, height = 10.dp)) {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.5f
                )
                drawCircle(color, 4f, Offset(size.width / 2, size.height / 2))
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = TextSecondary)
    }
}

// ============ 睡眠时长条形统计图 ============
@Composable
private fun SleepBarChart(sleepRecords: List<SleepRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "睡眠时长（近7天）",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 图例
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 10.dp)
                        .background(AccentTeal, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("睡眠时长", fontSize = 10.sp, color = TextSecondary)
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 1.dp)
                        .background(AccentOrange)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("推荐8小时", fontSize = 10.sp, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 生成最近7天的日期列表（始终显示完整7天）
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displaySdf = SimpleDateFormat("MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val last7Days = (6 downTo 0).map { daysAgo ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                sdf.format(cal.time)
            }

            // 将睡眠记录转为Map方便查找
            val recordMap = sleepRecords
                .filter { it.bedtime.isNotBlank() && it.waketime.isNotBlank() }
                .associateBy { it.date }

            // 生成7天完整数据，没有记录的日子为0
            val sleepData = last7Days.map { dateStr ->
                val dateLabel = dateStr.takeLast(5) // "MM-dd"
                val record = recordMap[dateStr]
                val hours = if (record != null) calculateSleepHours(record.bedtime, record.waketime) else 0.0
                Pair(dateLabel, hours)
            }

            val hasData = sleepData.any { it.second > 0 }

            if (!hasData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无睡眠数据，记录后查看统计", color = TextHint, fontSize = 12.sp)
                }
            } else {
                // 睡眠统计摘要（只统计有数据的天）
                val validData = sleepData.filter { it.second > 0 }
                val avgHours = validData.map { it.second }.average()
                val maxHours = validData.maxOf { it.second }
                val minHours = validData.minOf { it.second }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SleepStatMini("平均", String.format("%.1fh", avgHours), AccentTeal)
                    SleepStatMini("最长", String.format("%.1fh", maxHours), PrimaryPink)
                    SleepStatMini("最短", String.format("%.1fh", minHours), AccentOrange)
                }

                // 条形图
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val paddingLeft = 55f
                    val paddingRight = 20f
                    val paddingTop = 10f
                    val paddingBottom = 45f
                    val chartHeight = height - paddingTop - paddingBottom
                    val chartWidth = width - paddingLeft - paddingRight

                    val maxVal = 14f  // Y轴最大14小时
                    val barCount = sleepData.size
                    val barWidth = (chartWidth / barCount) * 0.55f
                    val barSpacing = chartWidth / barCount

                    // Y轴刻度
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#999999")
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.RIGHT
                        isAntiAlias = true
                    }

                    for (tick in listOf(0, 4, 8, 12)) {
                        val ty = height - paddingBottom - (tick.toFloat() / maxVal) * chartHeight
                        // 横向网格线
                        drawLine(
                            color = Color(0xFFEEEEEE),
                            start = Offset(paddingLeft, ty),
                            end = Offset(width - paddingRight, ty),
                            strokeWidth = 1f
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "${tick}h",
                            paddingLeft - 10f,
                            ty + 8f,
                            textPaint
                        )
                    }

                    // 推荐8小时参考线（橙色）
                    val refY = height - paddingBottom - (8f / maxVal) * chartHeight
                    val dashLen = 10f
                    val gapLen = 6f
                    var dx = paddingLeft
                    while (dx < width - paddingRight) {
                        val endX = (dx + dashLen).coerceAtMost(width - paddingRight)
                        drawLine(
                            color = AccentOrange.copy(alpha = 0.6f),
                            start = Offset(dx, refY),
                            end = Offset(endX, refY),
                            strokeWidth = 2f
                        )
                        dx += dashLen + gapLen
                    }

                    // 画条形
                    val bottomLabelPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#888888")
                        textSize = 22f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    val valuePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#4DB6AC")
                        textSize = 22f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                    }

                    sleepData.forEachIndexed { index, (dateLabel, hours) ->
                        val centerX = paddingLeft + barSpacing * index + barSpacing / 2

                        if (hours > 0) {
                            val barH = (hours.toFloat().coerceIn(0f, maxVal) / maxVal) * chartHeight
                            val barTop = height - paddingBottom - barH
                            val barLeft = centerX - barWidth / 2

                            // 根据睡眠时长变色
                            val barColor = when {
                                hours < 6 -> PeriodRed.copy(alpha = 0.7f)  // 不足6小时偏红
                                hours < 7 -> AccentOrange.copy(alpha = 0.7f)  // 6-7小时偏橙
                                hours <= 9 -> AccentTeal.copy(alpha = 0.8f)  // 7-9小时健康绿
                                else -> Color(0xFF7986CB).copy(alpha = 0.7f)  // 超过9小时偏蓝紫
                            }

                            // 画圆角条形
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(barLeft, barTop),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(8f, 8f)
                            )

                            // 条形上方显示时长
                            drawContext.canvas.nativeCanvas.drawText(
                                String.format("%.1f", hours),
                                centerX,
                                barTop - 8f,
                                valuePaint
                            )
                        } else {
                            // 没有数据的日子画一个小灰色占位
                            val placeholderH = 4f
                            drawRoundRect(
                                color = Color(0xFFE0E0E0),
                                topLeft = Offset(centerX - barWidth / 2, height - paddingBottom - placeholderH),
                                size = Size(barWidth, placeholderH),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                        }

                        // 底部日期标签（始终显示）
                        drawContext.canvas.nativeCanvas.drawText(
                            dateLabel,
                            centerX,
                            height - 10f,
                            bottomLabelPaint
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepStatMini(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}

/** 计算睡眠时长（小时），支持跨日 */
private fun calculateSleepHours(bedtime: String, waketime: String): Double {
    return try {
        val bedParts = bedtime.split(":")
        val wakeParts = waketime.split(":")
        val bedMinutes = bedParts[0].toInt() * 60 + bedParts[1].toInt()
        val wakeMinutes = wakeParts[0].toInt() * 60 + wakeParts[1].toInt()

        val diffMinutes = if (wakeMinutes >= bedMinutes) {
            // 同一天（比如 06:00入睡 14:00起床，午睡场景）
            wakeMinutes - bedMinutes
        } else {
            // 跨日（比如 23:00入睡 07:00起床）
            (24 * 60 - bedMinutes) + wakeMinutes
        }
        diffMinutes / 60.0
    } catch (e: Exception) {
        0.0
    }
}
