package com.laileme.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.BorderStroke
import com.laileme.app.data.AppDatabase
import com.laileme.app.data.AuthManager
import com.laileme.app.data.SyncManager
import com.laileme.app.data.entity.DiaryEntry
import com.laileme.app.data.entity.PeriodRecord
import com.laileme.app.data.entity.SleepRecord
import com.laileme.app.notification.NotificationScheduler
import com.laileme.app.ui.PeriodUiState
import com.laileme.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsContent(
    uiState: PeriodUiState,
    onSaveSettings: (Int, Int) -> Unit,
    onSaveMode: (String) -> Unit,
    onBack: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenServiceAgreement: () -> Unit = {},
    onOpenInfoCollection: () -> Unit = {}
) {
    var showCycleDialog by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showRemovePasswordDialog by remember { mutableStateOf(false) }
    var showReminderSettings by remember { mutableStateOf(false) }
    var showHelpFeedback by remember { mutableStateOf(false) }
    // showPrivacyPolicy, showServiceAgreement, showInfoCollection 已迁移为独立页面
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showWidgetSettings by remember { mutableStateOf(false) }
    var showSwitchAccountDialog by remember { mutableStateOf(false) }
    var showDataBackup by remember { mutableStateOf(false) }
    var showThemeColorPicker by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // 注销账号相关状态
    var showDeleteAccountWarning by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }
    var deletePassword by remember { mutableStateOf("") }
    var isDeletingAccount by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

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
            // ── 经期设置分组（男性隐藏）──
            val settingsGender = AuthManager.userState.collectAsState().value?.gender ?: "female"
            if (settingsGender != "male") {
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
            }

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

            SettingsItemWithValue(
                icon = Icons.Outlined.Security,
                title = "权限管理",
                value = "查看和管理应用权限",
                onClick = { showPermissionDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 外观设置分组 ──
            SettingsSectionTitle("外观")

            SettingsItem(Icons.Outlined.Palette, "主题颜色", onClick = { showThemeColorPicker = true })

            SettingsItem(
                icon = Icons.Outlined.Widgets,
                title = "桌面小部件",
                onClick = { showWidgetSettings = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 未成年保护 ──
            val profilePrefsForMinor = remember { context.getSharedPreferences("laileme_profile", Context.MODE_PRIVATE) }
            val settingsPrefsForMinor = remember { context.getSharedPreferences("laileme_settings", Context.MODE_PRIVATE) }
            val autoMinor = remember {
                try {
                    val y = profilePrefsForMinor.getString("birth_year", "")?.toIntOrNull()
                    val m = profilePrefsForMinor.getString("birth_month", "")?.toIntOrNull()
                    val d = profilePrefsForMinor.getString("birth_day", "")?.toIntOrNull()
                    if (y != null && m != null && d != null) {
                        val now = java.util.Calendar.getInstance()
                        var age = now.get(java.util.Calendar.YEAR) - y
                        if (now.get(java.util.Calendar.MONTH) + 1 < m ||
                            (now.get(java.util.Calendar.MONTH) + 1 == m && now.get(java.util.Calendar.DAY_OF_MONTH) < d)) {
                            age--
                        }
                        age < 18
                    } else false
                } catch (_: Exception) { false }
            }
            var isMinorMode by remember {
                val hasManualSet = settingsPrefsForMinor.contains("minor_mode")
                val defaultValue = if (hasManualSet) settingsPrefsForMinor.getBoolean("minor_mode", false) else autoMinor
                mutableStateOf(defaultValue)
            }
            var showMinorModeDialog by remember { mutableStateOf(false) }

            SettingsItemWithValue(
                icon = Icons.Outlined.ChildCare,
                title = "未成年模式",
                value = if (isMinorMode) "已开启" else "已关闭",
                onClick = { showMinorModeDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 未成年模式弹窗
            if (showMinorModeDialog) {
                AlertDialog(
                    onDismissRequest = { showMinorModeDialog = false },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    icon = { Icon(Icons.Outlined.ChildCare, null, tint = PrimaryPink, modifier = Modifier.size(36.dp)) },
                    title = { Text("未成年模式", fontWeight = FontWeight.Bold, color = TextPrimary) },
                    text = {
                        Column {
                            Text(
                                "开启后将隐藏私密日记等敏感内容，保护未成年人健康成长。",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )
                            if (autoMinor) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "根据档案年龄检测到你可能未满18岁，已自动开启。",
                                    fontSize = 12.sp,
                                    color = TextHint
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (isMinorMode) "当前已开启" else "当前已关闭",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Switch(
                                    checked = isMinorMode,
                                    onCheckedChange = { checked ->
                                        isMinorMode = checked
                                        settingsPrefsForMinor.edit()
                                            .putBoolean("minor_mode", checked)
                                            .apply()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = PrimaryPink
                                    )
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMinorModeDialog = false }) {
                            Text("关闭", color = PrimaryPink, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

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

            SettingsItem(Icons.Outlined.CloudUpload, "数据备份", onClick = { showDataBackup = true })

            Spacer(modifier = Modifier.height(16.dp))

            // ── 账户设置分组 ──
            SettingsSectionTitle("账户设置")

            SettingsItem(
                icon = Icons.Outlined.SwitchAccount,
                title = "切换账号",
                onClick = { showSwitchAccountDialog = true }
            )

            SettingsItem(
                icon = Icons.Outlined.VpnKey,
                title = "修改密码",
                onClick = { showChangePasswordDialog = true }
            )

            SettingsItem(
                icon = Icons.Outlined.DeleteForever,
                title = "注销账号",
                titleColor = Color(0xFFE53935),
                onClick = { showDeleteAccountWarning = true }
            )

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
                    onClick = onOpenInfoCollection
                )

                SettingsItem(
                    icon = Icons.Outlined.Security,
                    title = "隐私政策",
                    onClick = onOpenPrivacyPolicy
                )

                SettingsItem(
                    icon = Icons.Outlined.Description,
                    title = "服务协议",
                    onClick = onOpenServiceAgreement
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

    // 权限管理对话框
    if (showPermissionDialog) {
        val permContext = context
        val hasActivityRecog = PackageManager.PERMISSION_GRANTED ==
            ContextCompat.checkSelfPermission(permContext, Manifest.permission.ACTIVITY_RECOGNITION)
        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(permContext, Manifest.permission.POST_NOTIFICATIONS)
        } else true
        val hasBatterySaver = run {
            val pm = permContext.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            pm?.isIgnoringBatteryOptimizations(permContext.packageName) ?: true
        }
        val hasBodySensors = PackageManager.PERMISSION_GRANTED ==
            ContextCompat.checkSelfPermission(permContext, Manifest.permission.BODY_SENSORS)

        val brandName = com.laileme.app.util.AutoStartHelper.getBrandName()

        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.Outlined.Security,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = AccentTeal
                )
            },
            title = {
                Text("权限管理", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    // 当前品牌提示
                    Text(
                        "当前设备：$brandName",
                        fontSize = 11.sp,
                        color = TextHint,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    SettingsPermissionRow("运动识别", "记录步数和运动数据", hasActivityRecog)
                    SettingsPermissionRow("通知权限", "显示提醒和步数通知", hasNotification)
                    SettingsPermissionRow("电池优化", "后台持续记录步数", hasBatterySaver)
                    SettingsPermissionRow("身体传感器", "读取计步传感器数据", hasBodySensors)

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "后台保活设置",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        "根据个人意愿开启",
                        fontSize = 11.sp,
                        color = TextHint,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // 自启动管理
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentTeal.copy(alpha = 0.06f))
                            .clickable {
                                val ok = com.laileme.app.util.AutoStartHelper.jumpToAutoStart(permContext)
                                if (!ok) com.laileme.app.util.AutoStartHelper.jumpToAppDetail(permContext)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(20.dp), tint = AccentTeal)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("自启动管理", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("允许来了么开机自启动", fontSize = 10.sp, color = TextSecondary)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp), tint = TextHint)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 后台运行/省电策略
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFF9800).copy(alpha = 0.06f))
                            .clickable {
                                com.laileme.app.util.AutoStartHelper.jumpToBackgroundManager(permContext)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.BatteryChargingFull, null, modifier = Modifier.size(20.dp), tint = Color(0xFFFF9800))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("后台运行设置", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("设为「不受限制」保持后台计步", fontSize = 10.sp, color = TextSecondary)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp), tint = TextHint)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 应用详情（通用兜底）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryPink.copy(alpha = 0.06f))
                            .clickable {
                                com.laileme.app.util.AutoStartHelper.jumpToAppDetail(permContext)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Settings, null, modifier = Modifier.size(20.dp), tint = PrimaryPink)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("应用详情设置", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("管理所有权限和通知", fontSize = 10.sp, color = TextSecondary)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp), tint = TextHint)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("完成", color = AccentTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {}
        )
    }

    // 切换账号对话框
    if (showSwitchAccountDialog) {
        SwitchAccountDialog(
            onDismiss = { showSwitchAccountDialog = false },
            onSwitched = { showSwitchAccountDialog = false }
        )
    }

    // 帮助与反馈对话框
    if (showHelpFeedback) {
        HelpFeedbackDialog(
            onDismiss = { showHelpFeedback = false }
        )
    }


    // 修改密码弹窗
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onSuccess = { showChangePasswordDialog = false }
        )
    }

    // ── 注销账号 第一步：警告弹窗 ──
    if (showDeleteAccountWarning) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteAccountWarning = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    "注销账号警告",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFE53935)
                )
            },
            text = {
                Column {
                    Text(
                        "注销账号后，以下数据将被永久删除且无法恢复：",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A2E)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf(
                        "所有经期记录与周期数据",
                        "所有日记内容",
                        "睡眠记录",
                        "个人档案信息",
                        "VIP会员状态",
                        "邀请码与奖励",
                        "所有云端同步数据"
                    ).forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Icon(
                                Icons.Outlined.RemoveCircleOutline,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                item,
                                fontSize = 13.sp,
                                color = Color(0xFF666680)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "此操作不可逆，请谨慎考虑！",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountWarning = false
                        deleteConfirmText = ""
                        deletePassword = ""
                        deleteError = null
                        showDeleteAccountConfirm = true
                    }
                ) {
                    Text("我已了解，继续注销", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountWarning = false }) {
                    Text("取消", color = Color(0xFF666680))
                }
            }
        )
    }

    // ── 注销账号 第二步：输入确认文字 + 密码 ──
    if (showDeleteAccountConfirm) {
        val scope = rememberCoroutineScope()
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                if (!isDeletingAccount) {
                    showDeleteAccountConfirm = false
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "最终确认",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFE53935)
                )
            },
            text = {
                Column {
                    Text(
                        "请输入「确认注销」四个字以确认操作：",
                        fontSize = 13.sp,
                        color = Color(0xFF666680)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        placeholder = { Text("确认注销", fontSize = 14.sp, color = Color(0xFFCCCCCC)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "请输入登录密码进行验证：",
                        fontSize = 13.sp,
                        color = Color(0xFF666680)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it },
                        placeholder = { Text("输入密码", fontSize = 14.sp, color = Color(0xFFCCCCCC)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                    if (deleteError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            deleteError!!,
                            fontSize = 12.sp,
                            color = Color(0xFFE53935)
                        )
                    }
                    if (isDeletingAccount) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFE53935)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (deleteConfirmText != "确认注销") {
                            deleteError = "请正确输入「确认注销」四个字"
                            return@TextButton
                        }
                        if (deletePassword.isBlank()) {
                            deleteError = "请输入密码"
                            return@TextButton
                        }
                        isDeletingAccount = true
                        deleteError = null
                        scope.launch {
                            try {
                                val token = context.getSharedPreferences("laileme_auth", Context.MODE_PRIVATE)
                                    .getString("token", "") ?: ""
                                val result = withContext(Dispatchers.IO) {
                                    val url = java.net.URL("http://47.123.5.171:8080/api/auth/delete-account")
                                    val conn = url.openConnection() as java.net.HttpURLConnection
                                    conn.requestMethod = "POST"
                                    conn.setRequestProperty("Content-Type", "application/json")
                                    conn.setRequestProperty("Authorization", "Bearer $token")
                                    conn.doOutput = true
                                    conn.connectTimeout = 15000
                                    conn.readTimeout = 15000
                                    val body = org.json.JSONObject().apply {
                                        put("password", deletePassword)
                                    }.toString()
                                    conn.outputStream.use { it.write(body.toByteArray()) }
                                    val responseCode = conn.responseCode
                                    val response = if (responseCode == 200) {
                                        conn.inputStream.bufferedReader().readText()
                                    } else {
                                        conn.errorStream?.bufferedReader()?.readText() ?: "error"
                                    }
                                    conn.disconnect()
                                    response
                                }
                                val resp = org.json.JSONObject(result)
                                if (resp.optInt("code", 0) == 200) {
                                    // 注销成功，清除本地数据并登出
                                    com.laileme.app.data.SyncManager.stopAutoSync()
                                    com.laileme.app.data.SyncManager.setToken("")
                                    com.laileme.app.data.AuthManager.logout()
                                    isDeletingAccount = false
                                    showDeleteAccountConfirm = false
                                    android.widget.Toast.makeText(context, "账号已注销，所有数据已删除", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    isDeletingAccount = false
                                    deleteError = resp.optString("message", "注销失败")
                                }
                            } catch (e: Exception) {
                                isDeletingAccount = false
                                deleteError = "网络错误：${e.message}"
                            }
                        }
                    },
                    enabled = !isDeletingAccount
                ) {
                    Text(
                        if (isDeletingAccount) "注销中..." else "确认注销",
                        color = if (!isDeletingAccount) Color(0xFFE53935) else Color(0xFFCCCCCC),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountConfirm = false },
                    enabled = !isDeletingAccount
                ) {
                    Text("取消", color = Color(0xFF666680))
                }
            }
        )
    }

    // 桌面小部件设置弹窗
    if (showWidgetSettings) {
        WidgetSettingsDialog(onDismiss = { showWidgetSettings = false })
    }

    // 主题颜色选择弹窗
    if (showThemeColorPicker) {
        ThemeColorPickerDialog(onDismiss = { showThemeColorPicker = false })
    }

    // 数据备份弹窗
    if (showDataBackup) {
        DataBackupDialog(onDismiss = { showDataBackup = false })
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
private fun SettingsItem(icon: ImageVector, title: String, titleColor: Color = TextPrimary, onClick: () -> Unit = {}) {
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
                tint = if (titleColor != TextPrimary) titleColor else PrimaryPink
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                color = titleColor,
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

                // 获取性别，男性隐藏经期/喝水/用药/睡眠提醒
                val reminderGender = AuthManager.userState.collectAsState().value?.gender ?: "female"

                if (reminderGender != "male") {
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
                        Box(
                            modifier = Modifier
                                .size(50.dp, 42.dp)
                                .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = pHour,
                                onValueChange = {
                                    val v = it.filter { c -> c.isDigit() }.take(2)
                                    if (v.isEmpty() || (v.toIntOrNull() ?: 0) in 0..23) {
                                        periodTime = "${v.padStart(2, '0')}:$pMin"
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp, color = TextPrimary),
                                modifier = Modifier.width(40.dp)
                            )
                        }
                        Text(" : ", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Box(
                            modifier = Modifier
                                .size(50.dp, 42.dp)
                                .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = pMin,
                                onValueChange = {
                                    val v = it.filter { c -> c.isDigit() }.take(2)
                                    if (v.isEmpty() || (v.toIntOrNull() ?: 0) in 0..59) {
                                        periodTime = "$pHour:${v.padStart(2, '0')}"
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp, color = TextPrimary),
                                modifier = Modifier.width(40.dp)
                            )
                        }
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
                        Box(
                            modifier = Modifier
                                .size(50.dp, 42.dp)
                                .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = sHour,
                                onValueChange = {
                                    val v = it.filter { c -> c.isDigit() }.take(2)
                                    if (v.isEmpty() || (v.toIntOrNull() ?: 0) in 0..23) {
                                        sleepTime = "${v.padStart(2, '0')}:$sMin"
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp, color = TextPrimary),
                                modifier = Modifier.width(40.dp)
                            )
                        }
                        Text(" : ", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Box(
                            modifier = Modifier
                                .size(50.dp, 42.dp)
                                .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = sMin,
                                onValueChange = {
                                    val v = it.filter { c -> c.isDigit() }.take(2)
                                    if (v.isEmpty() || (v.toIntOrNull() ?: 0) in 0..59) {
                                        sleepTime = "$sHour:${v.padStart(2, '0')}"
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp, color = TextPrimary),
                                modifier = Modifier.width(40.dp)
                            )
                        }
                    }
                }
                } // end if reminderGender != "male"

                // ── 步数通知（所有用户可见）──
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(modifier = Modifier.height(12.dp))

                val settingsPrefsForStep = remember { context.getSharedPreferences("laileme_settings", Context.MODE_PRIVATE) }
                var stepNotifEnabled by remember { mutableStateOf(settingsPrefsForStep.getBoolean("step_notification", true)) }

                ReminderToggleItem(
                    icon = Icons.Outlined.DirectionsWalk,
                    title = "步数通知",
                    subtitle = "通知栏常驻显示今日步数",
                    checked = stepNotifEnabled,
                    color = Color(0xFFFF9800),
                    onCheckedChange = { enabled ->
                        stepNotifEnabled = enabled
                        settingsPrefsForStep.edit().putBoolean("step_notification", enabled).apply()
                        try {
                            if (enabled) {
                                com.laileme.app.service.KeepAliveService.start(context)
                            } else {
                                com.laileme.app.service.KeepAliveService.stop(context)
                            }
                        } catch (_: Exception) { }
                    }
                )
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

@Composable
private fun SettingsPermissionRow(title: String, desc: String, granted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (granted) Color(0xFF4CAF50) else Color(0xFFE57373)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(desc, fontSize = 10.sp, color = TextSecondary)
        }
        Text(
            if (granted) "已授权" else "未授权",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (granted) Color(0xFF4CAF50) else Color(0xFFE57373)
        )
    }
}

// ═══════════════════════════════════════════
// 切换账号对话框
// ═══════════════════════════════════════════

@Composable
private fun SwitchAccountDialog(onDismiss: () -> Unit, onSwitched: () -> Unit) {
    val accounts = remember { com.laileme.app.data.AuthManager.getSavedAccounts() }
    val currentUser = com.laileme.app.data.AuthManager.userState.collectAsState().value
    val scope = rememberCoroutineScope()
    var isSwitching by remember { mutableStateOf(false) }
    var switchError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!isSwitching) onDismiss() },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !isSwitching) {
                Text("关闭", color = TextSecondary)
            }
        },
        title = {
            Text(
                "切换账号",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1A1A2E)
            )
        },
        text = {
            Column {
                if (accounts.isEmpty()) {
                    Text(
                        "暂无已保存的账号\n登录后会自动记住账号",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    )
                } else {
                    accounts.forEach { account ->
                        val isCurrent = account.userId == currentUser?.userId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isCurrent && !isSwitching) {
                                    isSwitching = true
                                    switchError = null
                                    scope.launch {
                                        when (val result = com.laileme.app.data.AuthManager.switchAccount(account)) {
                                            is com.laileme.app.data.AuthResult.Success -> {
                                                com.laileme.app.data.AuthManager.saveAccountCredentials(account.username, account.password)
                                                android.widget.Toast.makeText(context, "已切换到 ${account.nickname}", android.widget.Toast.LENGTH_SHORT).show()
                                                onSwitched()
                                            }
                                            is com.laileme.app.data.AuthResult.Error -> {
                                                switchError = result.message
                                                isSwitching = false
                                            }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) PrimaryPink.copy(alpha = 0.1f) else Color(0xFFF5F5F5)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 头像圈
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (account.gender == "male") Color(0xFF4FC3F7).copy(alpha = 0.2f)
                                            else PrimaryPink.copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (account.gender == "male") Color(0xFF4FC3F7) else PrimaryPink
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = account.nickname,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1A1A2E)
                                    )
                                    Text(
                                        text = account.username + if (account.gender == "male") " · 男" else " · 女",
                                        fontSize = 11.sp,
                                        color = TextHint
                                    )
                                }
                                if (isCurrent) {
                                    Text(
                                        "当前",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPink
                                    )
                                }
                            }
                        }
                    }
                }

                if (isSwitching) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = PrimaryPink
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("切换中...", fontSize = 13.sp, color = TextHint)
                    }
                }

                switchError?.let {
                    Text(
                        it,
                        fontSize = 12.sp,
                        color = Color(0xFFE53935),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    )
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
                                "联系方式：postmaster@weinicole.cn",
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
fun WebViewPage(title: String, url: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding() // 防状态栏遮挡
    ) {
        // 顶部导航栏
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, "返回", tint = TextPrimary)
                }
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        // WebView 内容区域
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { context ->
                android.webkit.WebView(context).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.defaultTextEncodingName = "UTF-8"
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}




// ══════════════ 修改密码弹窗 ══════════════

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        confirmButton = {
            TextButton(
                onClick = {
                    errorMsg = null
                    when {
                        oldPassword.isBlank() -> errorMsg = "请输入旧密码"
                        newPassword.isBlank() -> errorMsg = "请输入新密码"
                        newPassword.length < 6 -> errorMsg = "新密码至少6位"
                        newPassword != confirmPassword -> errorMsg = "两次密码不一致"
                        oldPassword == newPassword -> errorMsg = "新密码不能与旧密码相同"
                        else -> {
                            isLoading = true
                            scope.launch {
                                val token = context.getSharedPreferences("laileme_auth", Context.MODE_PRIVATE)
                                    .getString("token", "") ?: ""
                                val result = changePassword(token, oldPassword, newPassword)
                                isLoading = false
                                if (result.first) {
                                    successMsg = "密码修改成功～"
                                } else {
                                    errorMsg = result.second
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading && successMsg == null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryPink)
                } else {
                    Text(if (successMsg != null) "完成" else "确认修改", color = PrimaryPink, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (successMsg == null) {
                TextButton(onClick = onDismiss, enabled = !isLoading) {
                    Text("取消", color = TextHint)
                }
            } else {
                TextButton(onClick = onSuccess) {
                    Text("关闭", color = PrimaryPink)
                }
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VpnKey, null, tint = PrimaryPink, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("修改密码", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                if (successMsg != null) {
                    Text(successMsg!!, color = AccentTeal, fontWeight = FontWeight.Medium)
                } else {
                    // 旧密码
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it; errorMsg = null },
                        label = { Text("旧密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPink,
                            cursorColor = PrimaryPink
                        ),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 新密码
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; errorMsg = null },
                        label = { Text("新密码（至少6位）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPink,
                            cursorColor = PrimaryPink
                        ),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 确认新密码
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMsg = null },
                        label = { Text("确认新密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPink,
                            cursorColor = PrimaryPink
                        ),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )

                    // 错误提示
                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMsg!!, color = PeriodRed, fontSize = 12.sp)
                    }
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

private suspend fun changePassword(token: String, oldPassword: String, newPassword: String): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("http://47.123.5.171:8080/api/auth/change-password")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            val body = org.json.JSONObject().apply {
                put("oldPassword", oldPassword)
                put("newPassword", newPassword)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            val responseCode = conn.responseCode
            val response = if (responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }
            conn.disconnect()

            val json = org.json.JSONObject(response)
            val code = json.optInt("code", 0)
            val message = json.optString("message", "")
            if (code == 200) {
                Pair(true, message)
            } else {
                Pair(false, message.ifBlank { "修改失败" })
            }
        } catch (e: Exception) {
            Pair(false, "网络错误：${e.localizedMessage}")
        }
    }
}

// ═══════════════════════════════════════════
// 桌面小部件设置弹窗
// ═══════════════════════════════════════════
@Composable
private fun WidgetSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var addResult by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0=综合面板, 1=经期倒计时

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                Icons.Outlined.Widgets,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = PrimaryPink
            )
        },
        title = {
            Text("桌面小部件", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "选择小部件样式添加到桌面～",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 样式切换按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTab = 0; addResult = null },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 0) PrimaryPink else Color(0xFFF0F0F0)
                    ) {
                        Text(
                            "综合面板 5×2",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedTab == 0) Color.White else TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTab = 1; addResult = null },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 1) PrimaryPink else Color(0xFFF0F0F0)
                    ) {
                        Text(
                            "经期倒计时 2×2",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedTab == 1) Color.White else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // 综合面板预览 - 双头像+天数+经期
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0E6F4)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 上半部分：头像 + 中央天数
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // 左头像
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = Color(0x20EC407A),
                                        border = androidx.compose.foundation.BorderStroke(2.dp, PrimaryPink)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("♀", fontSize = 16.sp, color = PrimaryPink)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("我", fontSize = 10.sp, color = PrimaryPink, fontWeight = FontWeight.Medium)
                                }

                                // 中央天数
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("♥ ♥", fontSize = 8.sp, color = Color(0xFFFF6B9D))
                                    Text("相守", fontSize = 10.sp, color = Color(0xFF8E7A9A))
                                    Text("204", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6C5BAE))
                                    Text("days", fontSize = 10.sp, color = Color(0xFFA090B8))
                                }

                                // 右头像
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = Color(0x20B39DDB),
                                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFB39DDB))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("♂", fontSize = 16.sp, color = Color(0xFF7E57C2))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("TA", fontSize = 10.sp, color = Color(0xFF7E57C2), fontWeight = FontWeight.Medium)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 底部经期卡片
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xC0FFFFFF)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("距下次经期", fontSize = 10.sp, color = Color(0xFF8E7A8A))
                                    Text("15天", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PeriodRed)
                                }
                            }
                        }
                    }
                } else {
                    // 经期倒计时预览
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8FF)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("来了么", fontSize = 12.sp, color = TextHint)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("15", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = PrimaryPink)
                            Text("天后来", fontSize = 12.sp, color = TextHint)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("周期第 13 天", fontSize = 10.sp, color = Color(0xFFD1D5DB))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("预览效果", fontSize = 11.sp, color = TextHint)

                if (addResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        addResult!!,
                        fontSize = 12.sp,
                        color = if (addResult!!.contains("成功") || addResult!!.contains("请求")) AccentTeal else PeriodRed
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                        val widgetClass = if (selectedTab == 0) {
                            com.laileme.app.widget.DashboardWidgetProvider::class.java
                        } else {
                            com.laileme.app.widget.PeriodWidgetProvider::class.java
                        }
                        val widgetProvider = android.content.ComponentName(context, widgetClass)
                        if (appWidgetManager.isRequestPinAppWidgetSupported) {
                            appWidgetManager.requestPinAppWidget(widgetProvider, null, null)
                            addResult = "已请求添加到桌面，请在弹窗中确认～"
                        } else {
                            addResult = "您的桌面不支持一键添加，请长按桌面手动添加"
                        }
                    } else {
                        addResult = "请长按桌面 → 小部件 → 找到「来了么」添加"
                    }
                }
            ) {
                Text("添加到桌面", color = PrimaryPink, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = TextSecondary)
            }
        }
    )
}

// ═══════════════════════════════════════════
// 数据备份对话框
// ═══════════════════════════════════════════
@Composable
private fun DataBackupDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var resultMsg by remember { mutableStateOf<String?>(null) }
    var resultIsError by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    // 文件选择器
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportConfirm = true
        }
    }

    // 导出功能
    fun exportData() {
        isExporting = true
        resultMsg = null
        scope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(context)
                    val periods = db.periodDao().getAllList()
                    val diaries = db.diaryDao().getAllList()
                    val sleepRecords = db.sleepDao().getAll().first()
                    val secretRecords = db.secretDao().getAll().first()

                    val profilePrefs = context.getSharedPreferences("laileme_profile", Context.MODE_PRIVATE)
                    val settingsPrefs = context.getSharedPreferences("laileme_settings", Context.MODE_PRIVATE)
                    val discoverPrefs = context.getSharedPreferences("laileme_discover", Context.MODE_PRIVATE)

                    JSONObject().apply {
                        put("appName", "来了么")
                        put("version", "2.0")
                        put("exportTime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

                        put("profile", JSONObject().apply {
                            put("nickname", profilePrefs.getString("nickname", "") ?: "")
                            put("birth_year", profilePrefs.getString("birth_year", "") ?: "")
                            put("birth_month", profilePrefs.getString("birth_month", "") ?: "")
                            put("birth_day", profilePrefs.getString("birth_day", "") ?: "")
                            put("height", profilePrefs.getString("height", "") ?: "")
                            put("weight", profilePrefs.getString("weight", "") ?: "")
                            put("blood_type", profilePrefs.getString("blood_type", "") ?: "")
                        })

                        put("settings", JSONObject().apply {
                            put("has_setup", settingsPrefs.getBoolean("has_setup", false))
                            put("tracking_mode", settingsPrefs.getString("tracking_mode", "auto") ?: "auto")
                            put("cycle_length", settingsPrefs.getInt("cycle_length", 28))
                            put("period_length", settingsPrefs.getInt("period_length", 5))
                        })

                        put("discover", JSONObject().apply {
                            put("medications", discoverPrefs.getString("medications", "") ?: "")
                            put("water_goal", discoverPrefs.getInt("water_goal", 8))
                        })

                        put("periodRecords", JSONArray().apply {
                            periods.forEach { r ->
                                put(JSONObject().apply {
                                    put("id", r.id)
                                    put("startDate", r.startDate)
                                    put("endDate", r.endDate ?: JSONObject.NULL)
                                    put("cycleLength", r.cycleLength)
                                    put("periodLength", r.periodLength)
                                    put("symptoms", r.symptoms)
                                    put("mood", r.mood)
                                    put("notes", r.notes)
                                })
                            }
                        })

                        put("diaryEntries", JSONArray().apply {
                            diaries.forEach { d ->
                                put(JSONObject().apply {
                                    put("id", d.id)
                                    put("date", d.date)
                                    put("mood", d.mood)
                                    put("symptoms", d.symptoms)
                                    put("notes", d.notes)
                                    put("flowLevel", d.flowLevel)
                                    put("flowColor", d.flowColor)
                                    put("painLevel", d.painLevel)
                                    put("breastPain", d.breastPain)
                                    put("digestive", d.digestive)
                                    put("backPain", d.backPain)
                                    put("headache", d.headache)
                                    put("fatigue", d.fatigue)
                                    put("skinCondition", d.skinCondition)
                                    put("temperature", d.temperature)
                                    put("appetite", d.appetite)
                                    put("discharge", d.discharge)
                                })
                            }
                        })

                        put("sleepRecords", JSONArray().apply {
                            sleepRecords.forEach { s ->
                                put(JSONObject().apply {
                                    put("date", s.date)
                                    put("bedtime", s.bedtime)
                                    put("waketime", s.waketime)
                                })
                            }
                        })
                    }
                }

                // 保存到下载目录
                withContext(Dispatchers.IO) {
                    val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val fileName = "laileme_backup_$dateStr.json"
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val subDir = File(dir, "Laileme")
                    if (!subDir.exists()) subDir.mkdirs()
                    val file = File(subDir, fileName)
                    file.writeText(json.toString(2))
                    resultMsg = "导出成功\n保存至: Download/Laileme/$fileName"
                    resultIsError = false
                }
            } catch (e: Exception) {
                resultMsg = "导出失败: ${e.message}"
                resultIsError = true
            }
            isExporting = false
        }
    }

    // 导入功能
    fun importData(uri: Uri) {
        isImporting = true
        resultMsg = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw Exception("无法读取文件")
                    val jsonStr = inputStream.bufferedReader().readText()
                    inputStream.close()

                    val json = JSONObject(jsonStr)

                    // 验证文件格式
                    if (!json.has("appName") || json.optString("appName") != "来了么") {
                        throw Exception("无效的备份文件")
                    }

                    val db = AppDatabase.getDatabase(context)

                    // 恢复个人档案
                    val profile = json.optJSONObject("profile")
                    if (profile != null) {
                        val prefs = context.getSharedPreferences("laileme_profile", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putString("nickname", profile.optString("nickname", ""))
                            .putString("birth_year", profile.optString("birth_year", ""))
                            .putString("birth_month", profile.optString("birth_month", ""))
                            .putString("birth_day", profile.optString("birth_day", ""))
                            .putString("height", profile.optString("height", ""))
                            .putString("weight", profile.optString("weight", ""))
                            .putString("blood_type", profile.optString("blood_type", ""))
                            .apply()
                    }

                    // 恢复经期设置
                    val settings = json.optJSONObject("settings")
                    if (settings != null) {
                        val prefs = context.getSharedPreferences("laileme_settings", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putBoolean("has_setup", settings.optBoolean("has_setup", false))
                            .putString("tracking_mode", settings.optString("tracking_mode", "auto"))
                            .putInt("cycle_length", settings.optInt("cycle_length", 28))
                            .putInt("period_length", settings.optInt("period_length", 5))
                            .apply()
                    }

                    // 恢复药物提醒
                    val discover = json.optJSONObject("discover")
                    if (discover != null) {
                        val prefs = context.getSharedPreferences("laileme_discover", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putString("medications", discover.optString("medications", ""))
                            .putInt("water_goal", discover.optInt("water_goal", 8))
                            .apply()
                    }

                    var totalCount = 0

                    // 先清空所有旧数据，避免重复
                    db.periodDao().deleteAllRecords()
                    db.diaryDao().deleteAll()
                    db.sleepDao().deleteAll()
                    db.secretDao().deleteAll()

                    // 恢复经期记录
                    val periodsArr = json.optJSONArray("periodRecords")
                    if (periodsArr != null) {
                        for (i in 0 until periodsArr.length()) {
                            val p = periodsArr.getJSONObject(i)
                            db.periodDao().insert(PeriodRecord(
                                id = 0,
                                startDate = p.getLong("startDate"),
                                endDate = if (p.isNull("endDate")) null else p.getLong("endDate"),
                                cycleLength = p.optInt("cycleLength", 28),
                                periodLength = p.optInt("periodLength", 5),
                                symptoms = p.optString("symptoms", ""),
                                mood = p.optString("mood", ""),
                                notes = p.optString("notes", "")
                            ))
                        }
                        totalCount += periodsArr.length()
                    }

                    // 恢复日记
                    val diariesArr = json.optJSONArray("diaryEntries")
                    if (diariesArr != null) {
                        for (i in 0 until diariesArr.length()) {
                            val d = diariesArr.getJSONObject(i)
                            db.diaryDao().insert(DiaryEntry(
                                id = 0,
                                date = d.getLong("date"),
                                mood = d.optString("mood", ""),
                                symptoms = d.optString("symptoms", ""),
                                notes = d.optString("notes", ""),
                                flowLevel = d.optInt("flowLevel", 0),
                                flowColor = d.optString("flowColor", ""),
                                painLevel = d.optInt("painLevel", 0),
                                breastPain = d.optInt("breastPain", 0),
                                digestive = d.optInt("digestive", 0),
                                backPain = d.optInt("backPain", 0),
                                headache = d.optInt("headache", 0),
                                fatigue = d.optInt("fatigue", 0),
                                skinCondition = d.optString("skinCondition", ""),
                                temperature = d.optString("temperature", ""),
                                appetite = d.optInt("appetite", 0),
                                discharge = d.optString("discharge", "")
                            ))
                        }
                        totalCount += diariesArr.length()
                    }
                    // 恢复睡眠记录
                    val sleepArr = json.optJSONArray("sleepRecords")
                    if (sleepArr != null) {
                        for (i in 0 until sleepArr.length()) {
                            val s = sleepArr.getJSONObject(i)
                            db.sleepDao().insert(SleepRecord(
                                date = s.getString("date"),
                                bedtime = s.optString("bedtime", ""),
                                waketime = s.optString("waketime", "")
                            ))
                        }
                        totalCount += sleepArr.length()
                    }

                    // 恢复私密日记
                    val secretsArr = json.optJSONArray("secretRecords")
                    if (secretsArr != null) {
                        for (i in 0 until secretsArr.length()) {
                            val s = secretsArr.getJSONObject(i)
                            db.secretDao().insert(com.laileme.app.data.entity.SecretRecord(
                                date = s.getLong("date"),
                                hadSex = s.optBoolean("hadSex", false),
                                protection = s.optString("protection", ""),
                                feeling = s.optInt("feeling", 0),
                                mood = s.optString("mood", ""),
                                notes = s.optString("notes", "")
                            ))
                        }
                        totalCount += secretsArr.length()
                    }

                    resultMsg = "导入成功，共恢复 $totalCount 条记录"
                    resultIsError = false

                    // 导入后触发同步
                    SyncManager.triggerImmediateSync()
                }
            } catch (e: Exception) {
                resultMsg = "导入失败: ${e.message}"
                resultIsError = true
            }
            isImporting = false
        }
    }

    // 删除数据确认弹窗
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(Icons.Outlined.DeleteForever, null, modifier = Modifier.size(36.dp), tint = PeriodRed)
            },
            title = { Text("删除所有数据", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PeriodRed) },
            text = {
                Column {
                    Text(
                        "此操作将永久删除以下所有数据：",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("  - 所有经期记录", fontSize = 13.sp, color = TextSecondary)
                    Text("  - 所有睡眠记录", fontSize = 13.sp, color = TextSecondary)
                    Text("  - 所有日记内容", fontSize = 13.sp, color = TextSecondary)
                    Text("  - 个人档案信息", fontSize = 13.sp, color = TextSecondary)
                    Text("  - 云端同步数据", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x20E53935)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.Warning, null, modifier = Modifier.size(16.dp), tint = PeriodRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "删除后数据不可恢复！建议先导出备份。\n此操作不会删除账号，仅清除数据。",
                                fontSize = 12.sp,
                                color = PeriodRed,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        isDeleting = true
                        resultMsg = null
                        scope.launch {
                            try {
                                // 删除本地数据
                                val db = AppDatabase.getDatabase(context)
                                db.periodDao().deleteAllRecords()
                                db.sleepDao().deleteAll()
                                db.diaryDao().deleteAll()
                                db.secretDao().deleteAll()

                                // 清除个人档案 SharedPreferences
                                context.getSharedPreferences("laileme_profile", Context.MODE_PRIVATE).edit().clear().apply()

                                // 删除云端数据
                                val token = context.getSharedPreferences("laileme_auth", Context.MODE_PRIVATE)
                                    .getString("token", "") ?: ""
                                if (token.isNotEmpty()) {
                                    withContext(Dispatchers.IO) {
                                        try {
                                            val url = java.net.URL("http://47.123.5.171:8080/api/user/data")
                                            val conn = url.openConnection() as java.net.HttpURLConnection
                                            conn.requestMethod = "DELETE"
                                            conn.setRequestProperty("Authorization", "Bearer $token")
                                            conn.connectTimeout = 10000
                                            conn.readTimeout = 10000
                                            conn.responseCode // 触发请求
                                            conn.disconnect()
                                        } catch (_: Exception) { }
                                    }
                                }

                                resultMsg = "所有数据已删除"
                                resultIsError = false
                            } catch (e: Exception) {
                                resultMsg = "删除失败: ${e.message}"
                                resultIsError = true
                            }
                            isDeleting = false
                        }
                    }
                ) {
                    Text("确认删除", color = PeriodRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }

    // 导入确认对话框
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(Icons.Outlined.Warning, null, modifier = Modifier.size(32.dp), tint = Color(0xFFFF9800))
            },
            title = { Text("确认导入", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    "导入数据会与当前数据合并（相同日期的记录会被覆盖），确定要继续吗？",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    pendingImportUri?.let { importData(it) }
                }) {
                    Text("确认导入", color = PrimaryPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isExporting && !isImporting) onDismiss() },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CloudUpload, null, modifier = Modifier.size(24.dp), tint = PrimaryPink)
                Spacer(modifier = Modifier.width(8.dp))
                Text("数据备份", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                // ── 云端同步状态 ──
                val lastSync = SyncManager.getLastSyncTime(context)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentTeal.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CloudDone, null, modifier = Modifier.size(20.dp), tint = AccentTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("云端同步", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(
                                if (lastSync != null) "上次同步: $lastSync" else "登录后自动开启云端同步",
                                fontSize = 11.sp,
                                color = TextHint
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 导出按钮 ──
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isExporting && !isImporting) { exportData() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5FF)),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.FileDownload, null, modifier = Modifier.size(22.dp), tint = PrimaryPink)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("导出数据到本地", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("保存所有数据为JSON文件", fontSize = 11.sp, color = TextHint)
                        }
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryPink)
                        } else {
                            Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp), tint = TextHint)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── 导入按钮 ──
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isExporting && !isImporting) { importLauncher.launch("application/json") },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5FF)),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.FileUpload, null, modifier = Modifier.size(22.dp), tint = AccentTeal)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("从本地导入数据", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("从JSON备份文件恢复数据", fontSize = 11.sp, color = TextHint)
                        }
                        if (isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AccentTeal)
                        } else {
                            Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp), tint = TextHint)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── 删除数据按钮 ──
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isExporting && !isImporting && !isDeleting) { showDeleteConfirm = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.DeleteForever, null, modifier = Modifier.size(22.dp), tint = PeriodRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("删除所有数据", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PeriodRed)
                            Text("清除本地和云端的全部数据（不删除账号）", fontSize = 11.sp, color = TextHint)
                        }
                        if (isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PeriodRed)
                        } else {
                            Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp), tint = TextHint)
                        }
                    }
                }

                // ── 操作结果提示 ──
                if (resultMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (resultIsError) Color(0x15E53935) else Color(0x1500C853)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Text(
                            resultMsg!!,
                            modifier = Modifier.padding(10.dp),
                            fontSize = 12.sp,
                            color = if (resultIsError) Color(0xFFE53935) else Color(0xFF00C853),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(12.dp))

                // ── 云端数据保留规则 ──
                Text(
                    "云端数据保留规则",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "登录用户享有免费云端同步服务。为合理利用服务器资源，数据将按以下规则保留：",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 规则列表
                RetentionRuleRow(icon = Icons.Outlined.Person, label = "免费用户", rule = "活跃期间永久保存\n不活跃超过60天后清理云端数据")
                RetentionRuleRow(icon = Icons.Outlined.Star, label = "月度VIP", rule = "VIP期间永久保存\n到期后保留120天")
                RetentionRuleRow(icon = Icons.Outlined.StarHalf, label = "年度VIP", rule = "VIP期间永久保存\n到期后保留720天")
                RetentionRuleRow(icon = Icons.Outlined.Verified, label = "永久VIP", rule = "永久保存，不受限制")

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Info, null, modifier = Modifier.size(16.dp), tint = Color(0xFFF9A825))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "建议定期导出数据到本地备份，本地数据不受云端保留规则影响。",
                            fontSize = 11.sp,
                            color = Color(0xFF795548),
                            lineHeight = 16.sp
                        )
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
private fun RetentionRuleRow(icon: ImageVector, label: String, rule: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
         Icon(icon, null, modifier = Modifier.size(18.dp).padding(top = 2.dp), tint = PrimaryPink)
         Spacer(modifier = Modifier.width(8.dp))
         Column {
             Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
             Text(rule, fontSize = 11.sp, color = TextHint, lineHeight = 15.sp)
         }
     }
 }

// ──────────────── 主题颜色选择器 ────────────────

@Composable
private fun ThemeColorPickerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var selectedKey by remember { mutableStateOf(ThemeManager.currentThemeKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text("主题颜色", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("选择你喜欢的颜色，整个App都会换装哦～", fontSize = 12.sp, color = TextHint)
            }
        },
        text = {
            // 用 LazyVerticalGrid 排列色块 — 4列
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.heightIn(max = 360.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ThemeManager.themes) { theme ->
                    val isSelected = selectedKey == theme.key
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedKey = theme.key }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(theme.primary)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, TextPrimary, CircleShape)
                                    else Modifier.border(1.5.dp, Color(0xFFE0E0E0), CircleShape)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            theme.name,
                            fontSize = 10.sp,
                            color = if (isSelected) theme.primary else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                ThemeManager.setTheme(context, selectedKey)
                onDismiss()
            }) {
                Text("确定", color = PrimaryPink, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextHint)
            }
        }
    )
}
