package com.laileme.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laileme.app.ui.theme.*

// ═══════════════════════════════════════════
// 数据定义
// ═══════════════════════════════════════════

data class LevelOption(val value: Int, val label: String)
data class StringOption(val value: String, val label: String, val color: Color? = null)

// ── 月经量 ──
val flowLevelOptions = listOf(
    LevelOption(0, "未记录"),
    LevelOption(1, "少"),
    LevelOption(2, "中"),
    LevelOption(3, "多")
)

// ── 经血颜色 ──
val flowColorOptions = listOf(
    StringOption("", "未记录", Color(0xFFE0E0E0)),
    StringOption("light_red", "浅红", Color(0xFFFF8A80)),
    StringOption("red", "正红", Color(0xFFFF1744)),
    StringOption("dark_red", "深红", Color(0xFFB71C1C)),
    StringOption("brown", "棕色", Color(0xFF795548)),
    StringOption("black", "黑色", Color(0xFF424242))
)

// ── 疼痛程度 ──
val painLevelOptions = listOf(
    LevelOption(0, "无"),
    LevelOption(1, "轻微"),
    LevelOption(2, "中等"),
    LevelOption(3, "较重"),
    LevelOption(4, "严重")
)

// ── 通用三级 ──
val threeLevelOptions = listOf(
    LevelOption(0, "无"),
    LevelOption(1, "轻微"),
    LevelOption(2, "明显"),
    LevelOption(3, "严重")
)

// ── 肠胃 ──
val digestiveOptions = listOf(
    LevelOption(0, "正常"),
    LevelOption(1, "不适"),
    LevelOption(2, "腹泻"),
    LevelOption(3, "便秘")
)

// ── 疲劳 ──
val fatigueOptions = listOf(
    LevelOption(0, "充沛"),
    LevelOption(1, "正常"),
    LevelOption(2, "有点累"),
    LevelOption(3, "很疲惫")
)

// ── 皮肤 ──
val skinOptions = listOf(
    StringOption("", "未记录"),
    StringOption("good", "很好"),
    StringOption("normal", "正常"),
    StringOption("oily", "出油"),
    StringOption("acne", "长痘"),
    StringOption("dry", "干燥")
)

// ── 食欲 ──
val appetiteOptions = listOf(
    LevelOption(0, "正常"),
    LevelOption(1, "增加"),
    LevelOption(2, "减少")
)

// ── 分泌物 ──
val dischargeOptions = listOf(
    StringOption("", "未记录"),
    StringOption("none", "无"),
    StringOption("clear", "透明"),
    StringOption("white", "白色"),
    StringOption("yellow", "黄色"),
    StringOption("sticky", "粘稠")
)

// ═══════════════════════════════════════════
// 身体状态记录区域
// ═══════════════════════════════════════════

@Composable
fun BodyStatusSection(
    flowLevel: Int,
    onFlowLevelChange: (Int) -> Unit,
    flowColor: String,
    onFlowColorChange: (String) -> Unit,
    painLevel: Int,
    onPainLevelChange: (Int) -> Unit,
    breastPain: Int,
    onBreastPainChange: (Int) -> Unit,
    digestive: Int,
    onDigestiveChange: (Int) -> Unit,
    backPain: Int,
    onBackPainChange: (Int) -> Unit,
    headache: Int,
    onHeadacheChange: (Int) -> Unit,
    fatigue: Int,
    onFatigueChange: (Int) -> Unit,
    skinCondition: String,
    onSkinConditionChange: (String) -> Unit,
    temperature: String,
    onTemperatureChange: (String) -> Unit,
    appetite: Int,
    onAppetiteChange: (Int) -> Unit,
    discharge: String,
    onDischargeChange: (String) -> Unit,
    initialExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initialExpanded) }

    // 统计已记录项数
    val recordedCount = listOf(
        flowLevel > 0,
        flowColor.isNotEmpty(),
        painLevel > 0,
        breastPain > 0,
        digestive > 0,
        backPain > 0,
        headache > 0,
        fatigue > 0,
        skinCondition.isNotEmpty(),
        temperature.isNotEmpty(),
        appetite > 0,
        discharge.isNotEmpty()
    ).count { it }

    Column {
        // 展开/折叠标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.MonitorHeart,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AccentTeal
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "身体状态",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                if (recordedCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentTeal.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "已记录 $recordedCount 项",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            color = AccentTeal
                        )
                    }
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = TextHint
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // ── 月经量 ──
                StatusCategory(
                    icon = Icons.Outlined.WaterDrop,
                    title = "月经量",
                    color = PeriodRed
                ) {
                    LevelSelector(
                        options = flowLevelOptions,
                        selectedValue = flowLevel,
                        onSelect = onFlowLevelChange,
                        activeColor = PeriodRed
                    )
                }

                // ── 经血颜色 ──
                StatusCategory(
                    icon = Icons.Outlined.Palette,
                    title = "经血颜色",
                    color = PeriodRed
                ) {
                    ColorSelector(
                        options = flowColorOptions,
                        selectedValue = flowColor,
                        onSelect = onFlowColorChange
                    )
                }

                // ── 疼痛程度 ──
                StatusCategory(
                    icon = Icons.Outlined.FlashOn,
                    title = "痛经程度",
                    color = AccentOrange
                ) {
                    LevelSelector(
                        options = painLevelOptions,
                        selectedValue = painLevel,
                        onSelect = onPainLevelChange,
                        activeColor = AccentOrange
                    )
                }

                // ── 胸部胀痛 ──
                StatusCategory(
                    icon = Icons.Outlined.FavoriteBorder,
                    title = "胸部胀痛",
                    color = PrimaryPink
                ) {
                    LevelSelector(
                        options = threeLevelOptions,
                        selectedValue = breastPain,
                        onSelect = onBreastPainChange,
                        activeColor = PrimaryPink
                    )
                }

                // ── 肠胃 ──
                StatusCategory(
                    icon = Icons.Outlined.Restaurant,
                    title = "肠胃状态",
                    color = AccentTeal
                ) {
                    LevelSelector(
                        options = digestiveOptions,
                        selectedValue = digestive,
                        onSelect = onDigestiveChange,
                        activeColor = AccentTeal
                    )
                }

                // ── 腰腹痛 ──
                StatusCategory(
                    icon = Icons.Outlined.Accessibility,
                    title = "腰腹痛",
                    color = AccentOrange
                ) {
                    LevelSelector(
                        options = threeLevelOptions,
                        selectedValue = backPain,
                        onSelect = onBackPainChange,
                        activeColor = AccentOrange
                    )
                }

                // ── 头痛 ──
                StatusCategory(
                    icon = Icons.Outlined.Psychology,
                    title = "头痛",
                    color = Color(0xFF9575CD)
                ) {
                    LevelSelector(
                        options = threeLevelOptions,
                        selectedValue = headache,
                        onSelect = onHeadacheChange,
                        activeColor = Color(0xFF9575CD)
                    )
                }

                // ── 疲劳 ──
                StatusCategory(
                    icon = Icons.Outlined.BatteryChargingFull,
                    title = "精力/疲劳",
                    color = AccentBlue
                ) {
                    LevelSelector(
                        options = fatigueOptions,
                        selectedValue = fatigue,
                        onSelect = onFatigueChange,
                        activeColor = AccentBlue
                    )
                }

                // ── 皮肤 ──
                StatusCategory(
                    icon = Icons.Outlined.Face,
                    title = "皮肤状态",
                    color = Color(0xFFFF8A65)
                ) {
                    StringSelector(
                        options = skinOptions,
                        selectedValue = skinCondition,
                        onSelect = onSkinConditionChange,
                        activeColor = Color(0xFFFF8A65)
                    )
                }

                // ── 体温 ──
                StatusCategory(
                    icon = Icons.Outlined.Thermostat,
                    title = "基础体温",
                    color = PeriodRed
                ) {
                    TemperatureInput(
                        value = temperature,
                        onValueChange = onTemperatureChange
                    )
                }

                // ── 食欲 ──
                StatusCategory(
                    icon = Icons.Outlined.LocalDining,
                    title = "食欲",
                    color = AccentTeal
                ) {
                    LevelSelector(
                        options = appetiteOptions,
                        selectedValue = appetite,
                        onSelect = onAppetiteChange,
                        activeColor = AccentTeal
                    )
                }

                // ── 分泌物 ──
                StatusCategory(
                    icon = Icons.Outlined.Opacity,
                    title = "分泌物",
                    color = AccentBlue
                ) {
                    StringSelector(
                        options = dischargeOptions,
                        selectedValue = discharge,
                        onSelect = onDischargeChange,
                        activeColor = AccentBlue
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════
// 子组件
// ═══════════════════════════════════════════

@Composable
private fun StatusCategory(
    icon: ImageVector,
    title: String,
    color: Color,
    content: @Composable () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

/** 数字等级选择器 */
@Composable
private fun LevelSelector(
    options: List<LevelOption>,
    selectedValue: Int,
    onSelect: (Int) -> Unit,
    activeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            val isSelected = selectedValue == option.value
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(if (isSelected) 0 else option.value) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) activeColor.copy(alpha = 0.18f) else Color(0xFFF5F5F5),
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(1.dp, activeColor.copy(alpha = 0.5f))
                } else null
            ) {
                Text(
                    option.label,
                    modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                    fontSize = 10.sp,
                    color = if (isSelected) activeColor else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/** 字符串选项选择器 */
@Composable
private fun StringSelector(
    options: List<StringOption>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    activeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            val isSelected = selectedValue == option.value
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(if (isSelected) "" else option.value) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) activeColor.copy(alpha = 0.18f) else Color(0xFFF5F5F5),
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(1.dp, activeColor.copy(alpha = 0.5f))
                } else null
            ) {
                Text(
                    option.label,
                    modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                    fontSize = 10.sp,
                    color = if (isSelected) activeColor else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/** 颜色选择器（带色块） */
@Composable
private fun ColorSelector(
    options: List<StringOption>,
    selectedValue: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            val isSelected = selectedValue == option.value
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(if (isSelected) "" else option.value) }
                    .background(
                        if (isSelected) PeriodRed.copy(alpha = 0.1f) else Color(0xFFF5F5F5),
                        RoundedCornerShape(8.dp)
                    )
                    .then(
                        if (isSelected) Modifier.border(
                            1.dp,
                            PeriodRed.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        ) else Modifier
                    )
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (option.color != null && option.value.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(option.color)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    option.label,
                    fontSize = 9.sp,
                    color = if (isSelected) PeriodRed else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

/** 体温输入框 */
@Composable
private fun TemperatureInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                // 只允许数字和一个小数点
                val filtered = input.filter { it.isDigit() || it == '.' }
                if (filtered.count { it == '.' } <= 1 && filtered.length <= 5) {
                    onValueChange(filtered)
                }
            },
            placeholder = { Text("例如 36.5", fontSize = 11.sp, color = TextHint) },
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PeriodRed,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color(0xFFFFFAFB),
                unfocusedContainerColor = Color(0xFFFAFAFA)
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("°C", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.width(8.dp))
        // 快捷按钮
        listOf("36.2", "36.5", "36.8", "37.0").forEach { temp ->
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onValueChange(temp) },
                shape = RoundedCornerShape(6.dp),
                color = if (value == temp) PeriodRed.copy(alpha = 0.18f) else Color(0xFFF0F0F0)
            ) {
                Text(
                    temp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    fontSize = 9.sp,
                    color = if (value == temp) PeriodRed else TextHint
                )
            }
            Spacer(modifier = Modifier.width(3.dp))
        }
    }
}
