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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laileme.app.ui.PeriodUiState
import com.laileme.app.ui.theme.*

@Composable
fun SettingsContent(
    uiState: PeriodUiState,
    onSaveSettings: (Int, Int) -> Unit,
    onSaveMode: (String) -> Unit,
    onBack: () -> Unit
) {
    var showCycleDialog by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryPink.copy(alpha = 0.1f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(20.dp),
                    tint = PrimaryPink
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "设置",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // 可滚动内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
                .padding(bottom = 80.dp)
        ) {
            // ── 经期设置分组 ──
            SettingsSectionTitle("经期设置")

            val modeText = if (uiState.trackingMode == "auto") "自动推算" else "手动输入"
            SettingsItemWithValue(
                icon = Icons.Outlined.Tune,
                title = "记录模式",
                value = modeText,
                onClick = { showModeDialog = true }
            )

            SettingsItemWithValue(
                icon = Icons.Outlined.DateRange,
                title = "周期设置",
                value = "周期${uiState.savedCycleLength}天 / 经期${uiState.savedPeriodLength}天",
                onClick = { showCycleDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 通知设置分组 ──
            SettingsSectionTitle("通知")

            SettingsItem(Icons.Outlined.Notifications, "提醒设置")

            Spacer(modifier = Modifier.height(16.dp))

            // ── 外观设置分组 ──
            SettingsSectionTitle("外观")

            SettingsItem(Icons.Outlined.Palette, "主题颜色")

            Spacer(modifier = Modifier.height(16.dp))

            // ── 数据与隐私分组 ──
            SettingsSectionTitle("数据与隐私")

            SettingsItem(Icons.Outlined.CloudUpload, "数据备份")

            Spacer(modifier = Modifier.height(16.dp))

            // ── 其他分组 ──
            SettingsSectionTitle("其他")

            SettingsItem(Icons.Outlined.HelpOutline, "帮助与反馈")
            SettingsItem(Icons.Outlined.Info, "关于来了么")

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // 修改记录模式对话框
    if (showModeDialog) {
        TrackingModeDialog(
            currentMode = uiState.trackingMode,
            onDismiss = { showModeDialog = false },
            onConfirm = { mode ->
                onSaveMode(mode)
                showModeDialog = false
            }
        )
    }

    // 修改周期设置对话框
    if (showCycleDialog) {
        CycleSettingsDialog(
            currentCycle = uiState.savedCycleLength,
            currentPeriod = uiState.savedPeriodLength,
            onDismiss = { showCycleDialog = false },
            onConfirm = { cycle, period ->
                onSaveSettings(cycle, period)
                showCycleDialog = false
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = PrimaryPink,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = PrimaryPink
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = TextHint
            )
        }
    }
}

@Composable
private fun SettingsItemWithValue(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = PrimaryPink
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 15.sp, color = TextPrimary)
                Text(text = value, fontSize = 11.sp, color = TextSecondary)
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = TextHint
            )
        }
    }
}

@Composable
private fun TrackingModeDialog(
    currentMode: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("记录模式", color = PrimaryPink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedMode = "auto" }
                        .background(
                            if (selectedMode == "auto") PrimaryPink.copy(alpha = 0.1f)
                            else Color.Transparent
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == "auto",
                        onClick = { selectedMode = "auto" },
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryPink)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("自动推算", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text("根据历史记录自动计算周期", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedMode = "manual" }
                        .background(
                            if (selectedMode == "manual") AccentTeal.copy(alpha = 0.1f)
                            else Color.Transparent
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == "manual",
                        onClick = { selectedMode = "manual" },
                        colors = RadioButtonDefaults.colors(selectedColor = AccentTeal)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("手动输入", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text("使用固定的周期和经期天数", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedMode) }) {
                Text("保存", color = PrimaryPink, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun CycleSettingsDialog(
    currentCycle: Int,
    currentPeriod: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var cycleLength by remember { mutableStateOf(currentCycle.toString()) }
    var periodLength by remember { mutableStateOf(currentPeriod.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("周期设置", color = PrimaryPink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column {
                Text(
                    "修改后将应用于新的经期记录",
                    color = TextSecondary, fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = cycleLength,
                    onValueChange = { cycleLength = it.filter { c -> c.isDigit() } },
                    label = { Text("月经周期（天）", fontSize = 12.sp) },
                    placeholder = { Text("通常21-35天", fontSize = 11.sp, color = TextHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPink,
                        focusedLabelColor = PrimaryPink
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = periodLength,
                    onValueChange = { periodLength = it.filter { c -> c.isDigit() } },
                    label = { Text("经期天数（天）", fontSize = 12.sp) },
                    placeholder = { Text("通常3-7天", fontSize = 11.sp, color = TextHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPink,
                        focusedLabelColor = PrimaryPink
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cycle = (cycleLength.toIntOrNull() ?: 28).coerceIn(15, 60)
                val period = (periodLength.toIntOrNull() ?: 5).coerceIn(1, 15)
                onConfirm(cycle, period)
            }) {
                Text("保存", color = PrimaryPink, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}
