package com.laileme.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.laileme.app.notification.NotificationScheduler
import com.laileme.app.ui.PeriodUiState
import com.laileme.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun SettingsContent(
    uiState: PeriodUiState,
    onSaveSettings: (Int, Int) -> Unit,
    onSaveMode: (String) -> Unit,
    onBack: () -> Unit
) {
    var showCycleDialog by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showRemovePasswordDialog by remember { mutableStateOf(false) }
    var showReminderSettings by remember { mutableStateOf(false) }
    var showHelpFeedback by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showServiceAgreement by remember { mutableStateOf(false) }
    var showInfoCollection by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val passwordPrefs = remember { context.getSharedPreferences("laileme_settings", Context.MODE_PRIVATE) }
    val reminderPrefs = remember { context.getSharedPreferences("laileme_reminders", Context.MODE_PRIVATE) }
    var isPasswordEnabled by remember {
        mutableStateOf(!passwordPrefs.getString("app_password", "").isNullOrEmpty())
    }

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

            val masterSwitch = reminderPrefs.getBoolean("notification_master", false)
            val periodRemind = reminderPrefs.getBoolean("period_remind", true)
            val waterRemind = reminderPrefs.getBoolean("water_remind", false)
            val medRemind = reminderPrefs.getBoolean("med_remind", false)
            val sleepRemind = reminderPrefs.getBoolean("sleep_remind", false)
            val enabledCount = listOf(periodRemind, waterRemind, medRemind, sleepRemind).count { it }

            val notifValue = if (!masterSwitch) {
                "通知已关闭"
            } else if (enabledCount > 0) {
                "已开启 ${enabledCount} 项提醒"
            } else {
                "通知已开启 · 未设置具体提醒"
            }

            SettingsItemWithValue(
                icon = Icons.Outlined.Notifications,
                title = "提醒设置",
                value = notifValue,
                onClick = { showReminderSettings = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 外观设置分组 ──
            SettingsSectionTitle("外观")

            SettingsItem(Icons.Outlined.Palette, "主题颜色")

            Spacer(modifier = Modifier.height(16.dp))

            // ── 数据与隐私分组 ──
            SettingsSectionTitle("数据与隐私")

            SettingsItemWithValue(
                icon = Icons.Outlined.Lock,
                title = "密码锁",
                value = if (isPasswordEnabled) "已启用 · 点击关闭" else "未启用 · 点击设置",
                onClick = {
                    if (isPasswordEnabled) {
                        showRemovePasswordDialog = true
                    } else {
                        showPasswordDialog = true
                    }
                }
            )

            SettingsItem(Icons.Outlined.CloudUpload, "数据备份")

            Spacer(modifier = Modifier.height(16.dp))

            // ── 其他分组 ──
            SettingsSectionTitle("其他")

            SettingsItem(
                icon = Icons.Outlined.HelpOutline,
                title = "帮助与反馈",
                onClick = { showHelpFeedback = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 法律与合规 ──
            SettingsSectionTitle("法律与合规")

            SettingsItem(
                icon = Icons.Outlined.ListAlt,
                title = "个人信息收集清单",
                onClick = { showInfoCollection = true }
            )

            SettingsItem(
                icon = Icons.Outlined.Security,
                title = "隐私政策",
                onClick = { showPrivacyPolicy = true }
            )

            SettingsItem(
                icon = Icons.Outlined.Description,
                title = "服务协议",
                onClick = { showServiceAgreement = true }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // 设置密码对话框
    if (showPasswordDialog) {
        SetPasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { password ->
                passwordPrefs.edit().putString("app_password", password).apply()
                isPasswordEnabled = true
                showPasswordDialog = false
            }
        )
    }

    // 关闭密码对话框
    if (showRemovePasswordDialog) {
        RemovePasswordDialog(
            savedPassword = passwordPrefs.getString("app_password", "") ?: "",
            onDismiss = { showRemovePasswordDialog = false },
            onConfirm = {
                passwordPrefs.edit().remove("app_password").apply()
                isPasswordEnabled = false
                showRemovePasswordDialog = false
            }
        )
    }

    // 提醒设置对话框
    if (showReminderSettings) {
        ReminderSettingsDialog(
            prefs = reminderPrefs,
            onDismiss = { showReminderSettings = false }
        )
    }

    // 帮助与反馈对话框
    if (showHelpFeedback) {
        HelpFeedbackDialog(
            onDismiss = { showHelpFeedback = false }
        )
    }

    // 个人信息收集清单
    if (showInfoCollection) {
        InfoCollectionDialog(onDismiss = { showInfoCollection = false })
    }

    // 隐私政策
    if (showPrivacyPolicy) {
        LegalTextDialog(title = "隐私政策", content = privacyPolicyContent, onDismiss = { showPrivacyPolicy = false })
    }

    // 服务协议
    if (showServiceAgreement) {
        LegalTextDialog(title = "服务协议", content = serviceAgreementContent, onDismiss = { showServiceAgreement = false })
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
private fun SettingsItem(icon: ImageVector, title: String, onClick: () -> Unit = {}) {
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

// ═══════════════════════════════════════════
// 密码锁对话框
// ═══════════════════════════════════════════

@Composable
private fun PinDotsRow(pinLength: Int, total: Int = 4) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < pinLength) PrimaryPink else Color(0xFFE0E0E0)
                    )
            )
        }
    }
}

@Composable
private fun PinPadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0xFFF5F5F5))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun SetPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1=输入, 2=确认
    var firstPin by remember { mutableStateOf("") }
    var currentPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        if (error) {
            kotlinx.coroutines.delay(1200)
            error = false
        }
    }

    // 当 PIN 输满4位时自动处理
    LaunchedEffect(currentPin) {
        if (currentPin.length == 4) {
            if (step == 1) {
                firstPin = currentPin
                currentPin = ""
                step = 2
            } else {
                if (currentPin == firstPin) {
                    onConfirm(currentPin)
                } else {
                    error = true
                    currentPin = ""
                    firstPin = ""
                    step = 1
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                "设置密码",
                color = PrimaryPink,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (step == 1) "请输入 4 位数字密码" else "请再次输入以确认",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                PinDotsRow(pinLength = currentPin.length)
                if (error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "两次输入不一致，请重新设置",
                        fontSize = 11.sp,
                        color = Color(0xFFE53935)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                // 数字键盘
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "⌫")
                )
                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "" -> Spacer(modifier = Modifier.size(56.dp))
                                "⌫" -> Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF5F5F5))
                                        .clickable {
                                            if (currentPin.isNotEmpty()) currentPin = currentPin.dropLast(1)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Backspace,
                                        contentDescription = "删除",
                                        modifier = Modifier.size(20.dp),
                                        tint = TextSecondary
                                    )
                                }
                                else -> PinPadButton(
                                    text = key,
                                    onClick = {
                                        if (currentPin.length < 4) currentPin += key
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun RemovePasswordDialog(
    savedPassword: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        if (error) {
            kotlinx.coroutines.delay(1200)
            error = false
        }
    }

    LaunchedEffect(currentPin) {
        if (currentPin.length == 4) {
            if (currentPin == savedPassword) {
                onConfirm()
            } else {
                error = true
                currentPin = ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                "关闭密码锁",
                color = PrimaryPink,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "请输入当前密码以关闭",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                PinDotsRow(pinLength = currentPin.length)
                if (error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "密码错误，请重试",
                        fontSize = 11.sp,
                        color = Color(0xFFE53935)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "⌫")
                )
                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "" -> Spacer(modifier = Modifier.size(56.dp))
                                "⌫" -> Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF5F5F5))
                                        .clickable {
                                            if (currentPin.isNotEmpty()) currentPin = currentPin.dropLast(1)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Backspace,
                                        contentDescription = "删除",
                                        modifier = Modifier.size(20.dp),
                                        tint = TextSecondary
                                    )
                                }
                                else -> PinPadButton(
                                    text = key,
                                    onClick = {
                                        if (currentPin.length < 4) currentPin += key
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

// ═══════════════════════════════════════════
// 提醒设置对话框
// ═══════════════════════════════════════════

@Composable
private fun ReminderSettingsDialog(
    prefs: android.content.SharedPreferences,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // 检查系统通知权限状态
    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    // 总开关默认值：没有权限则默认关闭
    var masterSwitch by remember {
        mutableStateOf(
            prefs.getBoolean("notification_master", false) && hasNotifPermission
        )
    }

    // 通知权限请求 launcher（需要在 masterSwitch 之后声明）
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (granted) {
            masterSwitch = true
        } else {
            // 用户拒绝了权限，关闭总开关
            masterSwitch = false
            prefs.edit().putBoolean("notification_master", false).apply()
            NotificationScheduler.reschedule(context)
        }
    }

    var periodRemind by remember { mutableStateOf(prefs.getBoolean("period_remind", true)) }
    var periodDaysBefore by remember { mutableStateOf(prefs.getInt("period_days_before", 2).toString()) }
    var periodTime by remember { mutableStateOf(prefs.getString("period_time", "09:00") ?: "09:00") }

    var waterRemind by remember { mutableStateOf(prefs.getBoolean("water_remind", false)) }
    var waterInterval by remember { mutableStateOf(prefs.getInt("water_interval", 2).toString()) }

    var medRemind by remember { mutableStateOf(prefs.getBoolean("med_remind", false)) }

    var sleepRemind by remember { mutableStateOf(prefs.getBoolean("sleep_remind", false)) }
    var sleepTime by remember { mutableStateOf(prefs.getString("sleep_time", "22:00") ?: "22:00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("提醒设置", color = PrimaryPink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // ═══ 通知总开关 ═══
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (masterSwitch) PrimaryPink.copy(alpha = 0.08f)
                            else Color(0xFFF5F5F5)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (masterSwitch) PrimaryPink else TextHint
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "通知总开关",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (masterSwitch) PrimaryPink else TextPrimary
                        )
                        Text(
                            if (masterSwitch) "已开启通知" else "关闭后将不会收到任何提醒",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = masterSwitch,
                        onCheckedChange = { wantOn ->
                            if (wantOn) {
                                // 打开总开关时检查权限
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    masterSwitch = true
                                }
                            } else {
                                masterSwitch = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = PrimaryPink,
                            checkedThumbColor = Color.White
                        )
                    )
                }

                // 权限被拒绝时的提示
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission && masterSwitch) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "⚠ 通知权限未授予，请在系统设置中开启",
                        fontSize = 11.sp,
                        color = Color(0xFFE53935),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // 当权限授予后自动打开总开关
                LaunchedEffect(hasNotifPermission) {
                    if (hasNotifPermission && !masterSwitch) {
                        // 用户刚授权，自动开启
                        val currentMaster = prefs.getBoolean("notification_master", false)
                        if (!currentMaster) {
                            // 首次请求权限成功，自动开启
                            masterSwitch = true
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!masterSwitch) {
                    // 总开关关闭时，显示提示文字
                    Text(
"开启通知总开关后可设置各项提醒",
                        fontSize = 13.sp,
                        color = TextHint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                } else {
                    // 总开关开启时显示分开关

                Text(
                    "开启后将在对应时间发送通知提醒",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── 经期提醒 ──
                ReminderToggleItem(
                    icon = Icons.Outlined.EventNote,
                    title = "经期提醒",
                    subtitle = "经期来临前提醒你做好准备",
                    checked = periodRemind,
                    color = PrimaryPink,
                    onCheckedChange = { periodRemind = it }
                )

                if (periodRemind) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 38.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("提前", fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = periodDaysBefore,
                            onValueChange = {
                                val v = it.filter { c -> c.isDigit() }.take(1)
                                periodDaysBefore = v
                            },
                            modifier = Modifier.width(50.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("天提醒", fontSize = 13.sp, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 38.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("提醒时间", fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        val pHour = periodTime.split(":").getOrNull(0) ?: "09"
                        val pMin = periodTime.split(":").getOrNull(1) ?: "00"
                        OutlinedTextField(
                            value = pHour,
                            onValueChange = {
                                val v = it.filter { c -> c.isDigit() }.take(2)
                                if (v.isEmpty() || (v.toIntOrNull() ?: 0) in 0..23) {
                                    periodTime = "${v.padStart(2, '0')}:$pMin"
                                }
                            },
                            modifier = Modifier.width(50.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp)
                        )
                        Text(" : ", fontWeight = FontWeight.Bold, color = TextPrimary)
                        OutlinedTextField(
                            value = pMin,
                            onValueChange = {
                                val v = it.filter { c -> c.isDigit() }.take(2)
                                if (v.isEmpty() || (v.toIntOrNull() ?: 0) in 0..59) {
                                    periodTime = "$pHour:${v.padStart(2, '0')}"
                                }
                            },
                            modifier = Modifier.width(50.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(modifier = Modifier.height(12.dp))

                // ── 喝水提醒 ──
                ReminderToggleItem(
                    icon = Icons.Outlined.LocalDrink,
                    title = "喝水提醒",
                    subtitle = "定时提醒你补充水分",
                    checked = waterRemind,
                    color = Color(0xFF42A5F5),
                    onCheckedChange = { waterRemind = it }
                )

                if (waterRemind) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 38.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("每隔", fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = waterInterval,
                            onValueChange = {
                                val v = it.filter { c -> c.isDigit() }.take(1)
                                waterInterval = v
                            },
                            modifier = Modifier.width(50.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("小时提醒一次", fontSize = 13.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(modifier = Modifier.height(12.dp))

                // ── 用药提醒 ──
                ReminderToggleItem(
                    icon = Icons.Outlined.LocalPharmacy,
                    title = "用药提醒",
                    subtitle = "按照用药记录的时间提醒服药",
                    checked = medRemind,
                    color = Color(0xFFE91E63),
                    onCheckedChange = { medRemind = it }
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(modifier = Modifier.height(12.dp))

                // ── 睡眠提醒 ──
                ReminderToggleItem(
                    icon = Icons.Outlined.Bedtime,
                    title = "睡眠提醒",
                    subtitle = "到点提醒你该休息啦",
                    checked = sleepRemind,
                    color = Color(0xFF5C6BC0),
                    onCheckedChange = { sleepRemind = it }
                )

                if (sleepRemind) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 38.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("提醒时间", fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        val sHour = sleepTime.split(":").getOrNull(0) ?: "22"
                        val sMin = sleepTime.split(":").getOrNull(1) ?: "00"
                        OutlinedTextField(
                            value = sHour,
                            onValueChange = {
                                val v = it.filter { c -> c.isDigit() }.take(2)
                                if (v.isEmpty() || (v.toIntOrNull() ?: 0) in 0..23) {
                                    sleepTime = "${v.padStart(2, '0')}:$sMin"
                                }
                            },
                            modifier = Modifier.width(50.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp)
                        )
                        Text(" : ", fontWeight = FontWeight.Bold, color = TextPrimary)
                        OutlinedTextField(
                            value = sMin,
                            onValueChange = {
                                val v = it.filter { c -> c.isDigit() }.take(2)
                                if (v.isEmpty() || (v.toIntOrNull() ?: 0) in 0..59) {
                                    sleepTime = "$sHour:${v.padStart(2, '0')}"
                                }
                            },
                            modifier = Modifier.width(50.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp)
                        )
                    }
                }
                } // end else masterSwitch
            }
        },
        confirmButton = {
            TextButton(onClick = {
                prefs.edit()
                    .putBoolean("notification_master", masterSwitch)
                    .putBoolean("period_remind", periodRemind)
                    .putInt("period_days_before", periodDaysBefore.toIntOrNull()?.coerceIn(1, 7) ?: 2)
                    .putString("period_time", periodTime)
                    .putBoolean("water_remind", waterRemind)
                    .putInt("water_interval", waterInterval.toIntOrNull()?.coerceIn(1, 8) ?: 2)
                    .putBoolean("med_remind", medRemind)
                    .putBoolean("sleep_remind", sleepRemind)
                    .putString("sleep_time", sleepTime)
                    .apply()
                // 重新调度通知任务
                NotificationScheduler.reschedule(context)
                onDismiss()
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

@Composable
private fun ReminderToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    color: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = if (checked) color else TextHint
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                subtitle,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = color,
                checkedThumbColor = Color.White
            )
        )
    }
}

// ═══════════════════════════════════════════
// 帮助与反馈对话框
// ═══════════════════════════════════════════

@Composable
private fun HelpFeedbackDialog(onDismiss: () -> Unit) {
    var feedbackText by remember { mutableStateOf("") }
    var feedbackSent by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=常见问题, 1=意见反馈
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        title = {
            Text("帮助与反馈", color = PrimaryPink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                // 标签切换
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(3.dp)
                ) {
                    listOf("常见问题", "意见反馈").forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedTab == index) Color.White else Color.Transparent
                                )
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) PrimaryPink else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // ── 常见问题 ──
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        FaqItem(
                            question = "如何记录经期？",
                            answer = "在首页点击「来了」按钮即可开始记录。经期结束后点击「结束」按钮完成本次记录。"
                        )
                        FaqItem(
                            question = "经期预测准确吗？",
                            answer = "预测基于你的历史记录进行计算。记录次数越多，预测越准确。建议坚持记录至少3个周期。"
                        )
                        FaqItem(
                            question = "睡眠数据在哪里查看？",
                            answer = "在「发现」页面可以记录每日睡眠时间，在「统计」页面可以查看近7天的睡眠时长条形图。"
                        )
                        FaqItem(
                            question = "数据安全吗？",
                            answer = "你的数据存储在安全的服务器上，密码经过加密处理。我们不会向第三方共享你的个人信息。"
                        )
                        FaqItem(
                            question = "如何设置密码锁？",
                            answer = "进入「设置」→「数据与隐私」→「密码锁」，设置4位数字密码即可保护你的隐私。"
                        )
                        FaqItem(
                            question = "忘记密码怎么办？",
                            answer = "目前需要清除应用数据来重置密码。未来版本会增加密码找回功能。"
                        )
                        FaqItem(
                            question = "如何修改个人信息？",
                            answer = "在「我的」页面点击「个人档案」，可以修改昵称、生日、身高、体重、血型等信息。"
                        )
                    }
                } else {
                    // ── 意见反馈 ──
                    if (feedbackSent) {
                        // 发送成功状态
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = AccentTeal
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "感谢你的反馈",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "我们会认真阅读每一条建议",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    } else {
                        Column {
                            Text(
                                "你的每一条建议对我们都很重要",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = feedbackText,
                                onValueChange = { feedbackText = it; sendError = null },
                                placeholder = {
                                    Text(
                                        "请描述你遇到的问题或建议...",
                                        fontSize = 13.sp,
                                        color = TextHint
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryPink,
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color(0xFFFAFAFA)
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                maxLines = 6
                            )

                            // 错误提示
                            if (sendError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    sendError ?: "",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE53935)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "联系方式：support@weinicole.cn",
                                fontSize = 11.sp,
                                color = TextHint
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        isSending = true
                                        sendError = null
                                        try {
                                            val success = withContext(Dispatchers.IO) {
                                                val url = URL("http://47.123.5.171:8080/api/feedback")
                                                val conn = url.openConnection() as HttpURLConnection
                                                conn.requestMethod = "POST"
                                                conn.setRequestProperty("Content-Type", "application/json")
                                                conn.doOutput = true
                                                conn.connectTimeout = 10000
                                                conn.readTimeout = 10000
                                                val body = """{"content":"${feedbackText.replace("\"", "\\\"").replace("\n", "\\n")}"}"""
                                                conn.outputStream.use { it.write(body.toByteArray()) }
                                                val code = conn.responseCode
                                                conn.disconnect()
                                                code == 200
                                            }
                                            if (success) {
                                                feedbackSent = true
                                            } else {
                                                sendError = "提交失败，请稍后再试"
                                            }
                                        } catch (e: Exception) {
                                            sendError = "网络错误：${e.message}"
                                        }
                                        isSending = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryPink,
                                    disabledContainerColor = PrimaryPink.copy(alpha = 0.3f)
                                ),
                                enabled = feedbackText.isNotBlank() && !isSending
                            ) {
                                if (isSending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("提交中...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        Icons.Outlined.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("提交反馈", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) PrimaryPink.copy(alpha = 0.05f) else Color(0xFFFAFAFA)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.HelpOutline,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = PrimaryPink
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = question,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = TextHint
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = answer,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(start = 24.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// 法律文本通用对话框
// ═══════════════════════════════════════════

@Composable
private fun LegalTextDialog(title: String, content: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        title = {
            Text(title, color = PrimaryPink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = content,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("我已知晓", fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ═══════════════════════════════════════════
// 个人信息收集清单对话框（表格）
// ═══════════════════════════════════════════

@Composable
private fun InfoCollectionDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        title = {
            Text("个人信息收集清单", color = PrimaryPink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "根据《个人信息保护法》相关规定，我们将在此公示本应用收集的个人信息详情：",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 表格
                val items = listOf(
                    InfoItem("用户名", "账号注册与登录", "必要", "服务器加密存储"),
                    InfoItem("密码", "账号安全验证", "必要", "BCrypt加密存储"),
                    InfoItem("昵称", "个性化显示", "可选", "服务器存储"),
                    InfoItem("生日", "年龄计算", "可选", "本地存储"),
                    InfoItem("身高/体重", "BMI计算", "可选", "本地存储"),
                    InfoItem("血型", "健康档案", "可选", "本地存储"),
                    InfoItem("经期记录", "周期预测与统计", "核心功能", "本地数据库"),
                    InfoItem("睡眠记录", "睡眠分析与统计", "核心功能", "本地数据库"),
                    InfoItem("日记内容", "心情记录", "可选", "本地数据库"),
                    InfoItem("设备信息", "问题排查与适配", "自动收集", "不上传"),
                    InfoItem("反馈内容", "改进产品服务", "用户主动提交", "服务器存储")
                )

                // 表头
                InfoTableRow(
                    col1 = "信息类型",
                    col2 = "用途",
                    col3 = "必要性",
                    col4 = "存储方式",
                    isHeader = true
                )

                items.forEach { item ->
                    InfoTableRow(
                        col1 = item.type,
                        col2 = item.purpose,
                        col3 = item.necessity,
                        col4 = item.storage,
                        isHeader = false
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "说明：\n" +
                    "1. 标注「必要」的信息为提供基础服务所需，拒绝提供将无法使用相关功能。\n" +
                    "2. 标注「可选」的信息用于提升体验，不提供不影响核心功能使用。\n" +
                    "3. 本地存储的数据不会上传至服务器，仅保存在你的设备中。\n" +
                    "4. 密码采用BCrypt单向加密，任何人（包括我们）均无法查看明文密码。",
                    fontSize = 11.sp,
                    color = TextHint,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("我已知晓", fontWeight = FontWeight.Bold)
            }
        }
    )
}

private data class InfoItem(
    val type: String,
    val purpose: String,
    val necessity: String,
    val storage: String
)

@Composable
private fun InfoTableRow(
    col1: String, col2: String, col3: String, col4: String,
    isHeader: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isHeader) PrimaryPink.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val weight = if (isHeader) FontWeight.Bold else FontWeight.Normal
        val color = if (isHeader) PrimaryPink else TextSecondary
        val size = if (isHeader) 11.sp else 10.sp

        Text(col1, fontSize = size, fontWeight = weight, color = color,
            modifier = Modifier.weight(1f))
        Text(col2, fontSize = size, fontWeight = weight, color = color,
            modifier = Modifier.weight(1.2f))
        Text(col3, fontSize = size, fontWeight = weight, color = color,
            modifier = Modifier.weight(0.8f))
        Text(col4, fontSize = size, fontWeight = weight, color = color,
            modifier = Modifier.weight(1f))
    }
    if (!isHeader) {
        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
    }
}

// ═══════════════════════════════════════════
// 隐私政策文本
// ═══════════════════════════════════════════

private val privacyPolicyContent = """
来了么 隐私政策

更新日期：2025年6月

我们深知个人信息对您的重要性，我们将按照法律法规要求，采取相应安全措施，保护您的个人信息安全。

一、我们收集的信息
为提供服务，我们可能收集以下信息：
1. 账号信息：用户名、昵称、密码（加密存储）；
2. 健康数据：经期记录、睡眠记录等您主动输入的数据；
3. 日记内容：您在日记功能中记录的文字内容；
4. 设备信息：设备型号、操作系统版本（仅用于适配和问题排查）。

二、信息的使用
我们收集的信息仅用于：
1. 提供和改善应用功能；
2. 保障账号安全；
3. 进行必要的数据分析以优化服务。

三、信息的存储与保护
1. 您的数据存储在安全的服务器上，我们采用业界通行的加密技术保护数据传输安全；
2. 密码经过加密处理存储，任何人（包括我们）都无法查看您的明文密码；
3. 我们会定期备份数据，防止数据丢失。

四、信息共享
我们承诺：
1. 未经您的同意，不会向第三方共享、出售您的个人信息；
2. 除非法律法规要求或政府机关依法要求，我们不会披露您的个人信息；
3. 我们不会将您的健康数据用于商业推广。

五、您的权利
您有权：
1. 查看、修改您的个人信息；
2. 删除您的账号及相关数据；
3. 撤回对本隐私政策的同意（撤回后将无法继续使用服务）。

六、未成年人保护
如果您是未满14周岁的未成年人，请在监护人的陪同下阅读本隐私政策，并在取得监护人同意后使用本应用。

七、隐私政策更新
我们可能会适时更新本隐私政策，更新后的政策将在应用内通知。

八、联系我们
如您对本隐私政策有任何疑问，请通过应用内反馈渠道或邮箱 support@weinicole.cn 联系我们。
""".trimIndent()

// ═══════════════════════════════════════════
// 服务协议文本
// ═══════════════════════════════════════════

private val serviceAgreementContent = """
来了么 服务协议

更新日期：2025年6月

欢迎使用「来了么」应用（以下简称"本应用"）。在使用本应用之前，请您仔细阅读以下条款。

一、服务说明
本应用为用户提供经期管理、睡眠记录、日记等健康生活辅助功能。本应用的所有功能仅供参考，不构成任何医疗建议。

二、账号注册与使用
1. 用户注册时应提供真实、准确的信息。
2. 用户应妥善保管账号密码，因用户保管不善导致的损失由用户自行承担。
3. 用户不得将账号转让、出借给他人使用。
4. 一个自然人只能注册一个账号，不得批量注册。

三、用户行为规范
用户在使用本应用时，不得：
1. 利用本应用从事违法违规活动；
2. 干扰、破坏本应用的正常运行；
3. 未经授权访问本应用的系统或数据；
4. 发布任何违法、有害、威胁、滥用、骚扰、侵权的内容；
5. 利用技术手段恶意攻击、入侵本应用服务器。

四、知识产权
本应用的所有内容，包括但不限于文字、图片、界面设计、程序代码等，均受知识产权法律保护，未经许可不得复制、修改或传播。

五、免责声明
1. 本应用提供的健康数据记录功能仅供参考，不能替代专业医疗诊断。
2. 因不可抗力、系统维护等原因导致的服务中断，本应用不承担责任。
3. 用户因违反本协议导致的任何损失，由用户自行承担。
4. 本应用不对用户数据的绝对安全性作出保证，但会尽最大努力保障数据安全。

六、账号注销
1. 用户有权申请注销账号，注销后相关数据将被删除且无法恢复。
2. 注销前请确保已备份重要数据。

七、协议修改
本应用有权在必要时修改本协议，修改后的协议将在应用内公布。用户继续使用本应用即视为同意修改后的协议。

八、法律适用与争议解决
本协议的解释与适用，以及与本协议有关的争议，均适用中华人民共和国法律。

九、联系方式
如有任何疑问，请通过应用内的反馈渠道或邮箱 support@weinicole.cn 与我们联系。
""".trimIndent()
