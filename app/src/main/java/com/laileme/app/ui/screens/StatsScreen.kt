package com.laileme.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laileme.app.data.entity.PeriodRecord
import com.laileme.app.ui.theme.*

@Composable
fun StatsScreen(records: List<PeriodRecord>) {
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
