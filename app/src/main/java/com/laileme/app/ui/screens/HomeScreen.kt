package com.laileme.app.ui.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.laileme.app.R
import com.laileme.app.data.AuthManager
import com.laileme.app.data.PaymentManager
import com.laileme.app.data.PartnerManager
import com.laileme.app.ui.PeriodUiState
import com.laileme.app.ui.normalizeDate
import com.laileme.app.ui.components.BunnyMascot
import com.laileme.app.ui.components.BunnyMascotLying
import androidx.compose.ui.graphics.vector.ImageVector
import com.laileme.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

private val periodTips = listOf(
    "经期前多补充铁质食物，如菠菜、红枣、黑芝麻哦~",
    "保持心情愉快，适当运动有助于缓解经期不适~",
    "经期注意保暖，避免着凉，热水袋是好帮手！",
    "多喝温水，少喝冷饮，对身体更好呢~",
    "规律作息有助于维持稳定的月经周期~",
    "经期前后适量补充维生素B6，可以缓解情绪波动~",
    "瑜伽和散步是经期友好的运动方式~",
    "经期饮食宜清淡，少吃辛辣刺激的食物~",
    "记录月经周期有助于了解自己的身体规律~",
    "排卵期前后注意观察身体变化，了解自己的生育窗口~",
    "经期可以适当吃些黑巧克力，有助于改善心情~",
    "充足的睡眠对月经规律非常重要~",
    "压力过大可能影响月经周期，记得放松自己~",
    "经期腹痛可以轻轻按摩腹部，顺时针方向哦~",
    "红糖姜茶是经期暖身的好选择~",
    "经期尽量避免剧烈运动和重体力劳动~",
    "多吃富含omega-3的食物有助于减轻痛经~",
    "经期前胸部胀痛是正常现象，不用太担心~",
    "保持个人卫生，勤换卫生用品很重要~",
    "如果经期异常，建议及时就医检查哦~",
    "香蕉富含钾元素，有助于缓解经期水肿~",
    "泡脚可以促进血液循环，缓解经期不适~",
    "经期适当补充钙质，有助于减少痉挛~",
    "每天保持30分钟的轻度运动对健康很有益~",
    "了解自己的PMS症状，提前做好应对准备~"
)

@Composable
fun HomeScreen(
    uiState: PeriodUiState,
    onAddPeriod: (Long) -> Unit,
    onEndPeriod: (Long) -> Unit,
    onReset: () -> Unit,
    onSaveSettings: (Int, Int) -> Unit,
    onSaveMode: (String) -> Unit
) {
    val hasRecord = uiState.latestRecord != null
    var showModeDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showManualSetupDialog by remember { mutableStateOf(false) }

    // ── 运动统计详情页 ──
    var showExerciseStats by remember { mutableStateOf(false) }

    // ── 运动健康模式 ──
    var isHealthMode by remember { mutableStateOf(false) }
    // ── 运动数据 ──
    val stepsGoal = 7500
    val caloriesGoal = 500 // kcal
    val exerciseTimeGoal = 60 // 分钟
    val context2 = LocalContext.current
    val currentUserId = remember {
        context2.getSharedPreferences("laileme_auth", android.content.Context.MODE_PRIVATE)
            .getString("user_id", "default") ?: "default"
    }
    val healthPrefs = remember { context2.getSharedPreferences("laileme_health_$currentUserId", android.content.Context.MODE_PRIVATE) }
    val todayKey = remember {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        sdf.format(java.util.Date())
    }
    var currentSteps by remember { mutableIntStateOf(healthPrefs.getInt("steps_$todayKey", 0)) }
    var currentCalories by remember { mutableIntStateOf(healthPrefs.getInt("calories_$todayKey", 0)) }
    var currentExerciseTime by remember { mutableIntStateOf(healthPrefs.getInt("exercise_$todayKey", 0)) }
    // 标记 Health Connect 是否成功读到了数据
    var healthConnectHasData by remember { mutableStateOf(false) }

    // ── 伴侣步数切换 ──
    var showPartnerSteps by remember { mutableStateOf(false) }
    var partnerSteps by remember { mutableIntStateOf(0) }
    var partnerCalories by remember { mutableIntStateOf(0) }
    var partnerExerciseTime by remember { mutableIntStateOf(0) }
    var partnerNickname by remember { mutableStateOf("伴侣") }
    var isLoadingPartner by remember { mutableStateOf(false) }
    // 显示用数据（根据切换状态选择自己/伴侣）
    val displaySteps = if (showPartnerSteps) partnerSteps else currentSteps
    val displayCalories = if (showPartnerSteps) partnerCalories else currentCalories
    val displayExerciseTime = if (showPartnerSteps) partnerExerciseTime else currentExerciseTime

    val healthScope = rememberCoroutineScope()

    // Health Connect 客户端
    val healthConnectClient = remember {
        try {
            androidx.health.connect.client.HealthConnectClient.getOrCreate(context2)
        } catch (e: Exception) { null }
    }
    val healthPermissions = remember {
        setOf(
            androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                androidx.health.connect.client.records.StepsRecord::class
            ),
            androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                androidx.health.connect.client.records.TotalCaloriesBurnedRecord::class
            ),
            androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                androidx.health.connect.client.records.ExerciseSessionRecord::class
            ),
            androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                androidx.health.connect.client.records.ActiveCaloriesBurnedRecord::class
            )
        )
    }

    // 读取 Health Connect 数据
    suspend fun readHealthData() {
        val client = healthConnectClient ?: return
        try {
            val now = java.time.Instant.now()
            val startOfDay = java.time.LocalDate.now()
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
            val timeRange = androidx.health.connect.client.time.TimeRangeFilter.between(startOfDay, now)

            val stepsResponse = client.aggregate(
                androidx.health.connect.client.request.AggregateRequest(
                    metrics = setOf(androidx.health.connect.client.records.StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = timeRange
                )
            )
            val steps = stepsResponse[androidx.health.connect.client.records.StepsRecord.COUNT_TOTAL]?.toInt() ?: 0

            if (steps > 0) {
                healthConnectHasData = true
                currentSteps = steps

                val calResponse = client.aggregate(
                    androidx.health.connect.client.request.AggregateRequest(
                        metrics = setOf(androidx.health.connect.client.records.ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                        timeRangeFilter = timeRange
                    )
                )
                val cal = calResponse[androidx.health.connect.client.records.ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                currentCalories = cal?.inKilocalories?.toInt() ?: (steps * 0.04f).toInt()

                val exerciseResponse = client.readRecords(
                    androidx.health.connect.client.request.ReadRecordsRequest(
                        recordType = androidx.health.connect.client.records.ExerciseSessionRecord::class,
                        timeRangeFilter = timeRange
                    )
                )
                val totalMinutes = exerciseResponse.records.sumOf { record ->
                    java.time.Duration.between(record.startTime, record.endTime).toMinutes()
                }.toInt()
                currentExerciseTime = if (totalMinutes > 0) totalMinutes else (steps / 100).coerceAtMost(120)

                healthPrefs.edit()
                    .putInt("steps_$todayKey", currentSteps)
                    .putInt("calories_$todayKey", currentCalories)
                    .putInt("exercise_$todayKey", currentExerciseTime)
                    .apply()
            }
        } catch (_: Exception) { }
    }

    val healthPermLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        healthScope.launch { readHealthData() }
    }

    // 权限就绪标志
    var sensorPermReady by remember { mutableStateOf(
        android.content.pm.PackageManager.PERMISSION_GRANTED ==
            androidx.core.content.ContextCompat.checkSelfPermission(
                context2, android.Manifest.permission.ACTIVITY_RECOGNITION
            )
    ) }

    // Health Connect 权限请求 launcher（真正请求授权）
    val healthConnectPermLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { grantedPerms ->
        healthScope.launch {
            if (grantedPerms.containsAll(healthPermissions)) {
                readHealthData()
                if (currentSteps > 0) {
                    android.widget.Toast.makeText(context2, "✅ 已从运动健康读取 $currentSteps 步", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ACTIVITY_RECOGNITION 运行时权限请求
    val activityPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        sensorPermReady = true
        isHealthMode = true
    }

    // 上传步数到云端（伴侣可见）
    suspend fun uploadStepsToCloud() {
        if (currentSteps <= 0) return
        val authPrefs = context2.getSharedPreferences("laileme_auth", android.content.Context.MODE_PRIVATE)
        val token = authPrefs.getString("token", "") ?: ""
        if (token.isBlank()) return
        try {
            withContext(Dispatchers.IO) {
                val url = java.net.URL("http://47.123.5.171:8080/api/sync/upload")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true
                val body = org.json.JSONObject().apply {
                    put("healthData", org.json.JSONObject().apply {
                        put(todayKey, org.json.JSONObject().apply {
                            put("steps", currentSteps)
                            put("calories", currentCalories)
                            put("exercise", currentExerciseTime)
                        })
                    })
                }
                conn.outputStream.bufferedWriter().use { it.write(body.toString()) }
                conn.responseCode
                conn.disconnect()
            }
        } catch (_: Exception) { }
    }

    // 获取伴侣步数数据
    suspend fun fetchPartnerSteps() {
        val authPrefs = context2.getSharedPreferences("laileme_auth", android.content.Context.MODE_PRIVATE)
        val token = authPrefs.getString("token", "") ?: ""
        if (token.isBlank()) return
        isLoadingPartner = true
        try {
            withContext(Dispatchers.IO) {
                val url = java.net.URL("http://47.123.5.171:8080/api/partner/data")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val json = org.json.JSONObject(body)
                    if (json.optInt("code") == 200) {
                        val data = json.optJSONObject("data")
                        if (data != null) {
                            partnerNickname = data.optString("partnerNickname", "伴侣")
                            val health = data.optJSONObject("healthData")
                            if (health != null) {
                                // 找今天的步数
                                val todaySdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                val todayStr = todaySdf.format(java.util.Date())
                                val todayData = health.optJSONObject(todayStr)
                                if (todayData != null) {
                                    partnerSteps = todayData.optInt("steps", 0)
                                    partnerCalories = todayData.optInt("calories", 0)
                                    partnerExerciseTime = todayData.optInt("exercise", todayData.optInt("exerciseTime", 0))
                                } else {
                                    partnerSteps = 0
                                    partnerCalories = 0
                                    partnerExerciseTime = 0
                                }
                            }
                        }
                    }
                }
                conn.disconnect()
            }
        } catch (_: Exception) { }
        isLoadingPartner = false
    }

    // ── 传感器计步（兜底数据源） ──
    val sensorManager = remember { context2.getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager }
    val stepSensor = remember { sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER) }

    DisposableEffect(isHealthMode, stepSensor, sensorPermReady) {
        if (!isHealthMode || stepSensor == null || sensorManager == null) {
            onDispose { }
        } else {
            val listener = object : android.hardware.SensorEventListener {
                var baseline = healthPrefs.getFloat("step_baseline_$todayKey", -1f)
                override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                    val totalSteps = event?.values?.firstOrNull() ?: return
                    if (baseline < 0f) {
                        baseline = totalSteps - healthPrefs.getInt("steps_$todayKey", 0).toFloat()
                        healthPrefs.edit().putFloat("step_baseline_$todayKey", baseline).apply()
                    }
                    val sensorSteps = (totalSteps - baseline).toInt().coerceAtLeast(0)
                    if (!healthConnectHasData || sensorSteps > currentSteps) {
                        if (sensorSteps > 0) {
                            currentSteps = sensorSteps
                            currentCalories = (sensorSteps * 0.04f).toInt()
                            currentExerciseTime = (sensorSteps / 100).coerceAtMost(120)
                            healthPrefs.edit()
                                .putInt("steps_$todayKey", currentSteps)
                                .putInt("calories_$todayKey", currentCalories)
                                .putInt("exercise_$todayKey", currentExerciseTime)
                                .apply()
                        }
                    }
                }
                override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, stepSensor, android.hardware.SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    // 进入运动模式时：请求 Health Connect 授权 → 读数据 → 立即同步 → 定时刷新上传
    LaunchedEffect(isHealthMode) {
        if (isHealthMode) {
            // 先尝试 Health Connect
            if (healthConnectClient != null) {
                val granted = try {
                    healthConnectClient.permissionController.getGrantedPermissions()
                } catch (_: Exception) { emptySet() }
                if (granted.containsAll(healthPermissions)) {
                    // 已有权限，直接读
                    readHealthData()
                } else {
                    // 没有权限，发起授权请求（会弹荣耀运动健康授权页）
                    healthConnectPermLauncher.launch(healthPermissions)
                }
            }
            // ★ 进入运动模式立即上传一次步数 + 从云端恢复步数
            try { uploadStepsToCloud() } catch (_: Exception) { }
            try {
                com.laileme.app.data.SyncManager.downloadAndRestore(context2)
                // 恢复后刷新本地显示
                val restoredSteps = healthPrefs.getInt("steps_$todayKey", 0)
                val restoredCalories = healthPrefs.getInt("calories_$todayKey", 0)
                val restoredExercise = healthPrefs.getInt("exercise_$todayKey", 0)
                if (restoredSteps > currentSteps) {
                    currentSteps = restoredSteps
                    currentCalories = restoredCalories
                    currentExerciseTime = restoredExercise
                }
            } catch (_: Exception) { }
            // 定时刷新（每30秒）+ 上传
            while (isHealthMode) {
                delay(30_000)
                try { readHealthData() } catch (_: Exception) { }
                try { uploadStepsToCloud() } catch (_: Exception) { }
            }
        }
    }
    // 电池优化权限提示对话框
    var showBatteryDialog by remember { mutableStateOf(false) }

    if (showBatteryDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.Outlined.BatteryChargingFull,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("允许后台运行", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    "为了更准确地记录您的步数，需要允许「来了么」不受电池优化限制。\n\n" +
                    "这样即使手机锁屏，也能持续为您记录步数哦～",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBatteryDialog = false
                    try {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        ).apply {
                            data = android.net.Uri.parse("package:${context2.packageName}")
                        }
                        context2.startActivity(intent)
                    } catch (_: Exception) { }
                }) {
                    Text("去设置", color = PrimaryPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryDialog = false }) {
                    Text("暂不", color = TextSecondary)
                }
            }
        )
    }

    var showAlreadyStartedDialog by remember { mutableStateOf(false) }
    var alreadyDaysInput by remember { mutableStateOf("") }
    var showDrawer by remember { mutableStateOf(false) }
    var showAnnouncement by remember { mutableStateOf(false) }
    var isSendingCare by remember { mutableStateOf(false) }

    // 公告数据
    val context = LocalContext.current
    val carePrefs = remember { context.getSharedPreferences("laileme_care", android.content.Context.MODE_PRIVATE) }
    val todayStr = remember {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        sdf.format(java.util.Date())
    }
    var hasCaredToday by remember { mutableStateOf(carePrefs.getString("last_care_date", "") == todayStr) }
    var showCareReceived by remember { mutableStateOf(false) }
    var careFromName by remember { mutableStateOf("") }
    data class Announcement(val title: String, val content: String, val time: String)
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var isLoadingAnnouncements by remember { mutableStateOf(false) }

    // 从服务器加载公告
    val scope = rememberCoroutineScope()
    fun loadAnnouncements(autoShow: Boolean = false) {
        isLoadingAnnouncements = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = URL("http://47.123.5.171:8080/api/announcements")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.inputStream.bufferedReader().readText()
                }
                // 解析 JSON 数组
                val jsonArray = org.json.JSONArray(result)
                val list = mutableListOf<Announcement>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(Announcement(
                        title = obj.optString("title", "公告"),
                        content = obj.optString("content", ""),
                        time = obj.optString("time", "")
                    ))
                }
                announcements = list

                // 自动弹出逻辑：用公告内容指纹判断是否有新公告
                if (autoShow && list.isNotEmpty()) {
                    val fingerprint = list.joinToString("|") { it.title + it.time }
                    val prefs = context.getSharedPreferences("laileme_announcements", Context.MODE_PRIVATE)
                    val lastSeen = prefs.getString("last_fingerprint", "") ?: ""
                    if (fingerprint != lastSeen) {
                        showAnnouncement = true
                        prefs.edit().putString("last_fingerprint", fingerprint).apply()
                    }
                }
            } catch (_: Exception) {
                if (!autoShow) {
                    announcements = listOf(Announcement("暂无公告", "当前没有新的公告内容～", ""))
                }
            } finally {
                isLoadingAnnouncements = false
            }
        }
    }

    // 启动时自动检查新公告 + 关怀通知
    LaunchedEffect(Unit) {
        delay(2000)
        loadAnnouncements(autoShow = true)

        // 女方检查是否收到新关怀
        val myGender = AuthManager.userState.value?.gender ?: "female"
        if (myGender == "female") {
            try {
                val token = context.getSharedPreferences("laileme_auth", Context.MODE_PRIVATE)
                    .getString("token", "") ?: ""
                if (token.isNotEmpty()) {
                    val careResult = withContext(Dispatchers.IO) {
                        val url = URL("http://47.123.5.171:8080/api/partner/care")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.setRequestProperty("Authorization", "Bearer $token")
                        conn.connectTimeout = 5000
                        conn.readTimeout = 5000
                        val resp = conn.inputStream.bufferedReader().readText()
                        conn.disconnect()
                        resp
                    }
                    val json = org.json.JSONObject(careResult)
                    if (json.optInt("code") == 200) {
                        val data = json.optJSONObject("data")
                        val unread = data?.optInt("unread", 0) ?: 0
                        if (unread > 0) {
                            val msgs = data?.optJSONArray("messages")
                            val firstName = msgs?.optJSONObject(0)?.optString("fromNickname", "TA") ?: "TA"
                            careFromName = firstName
                            showCareReceived = true
                            // 标记已读
                            withContext(Dispatchers.IO) {
                                try {
                                    val readUrl = URL("http://47.123.5.171:8080/api/partner/care/read")
                                    val readConn = readUrl.openConnection() as HttpURLConnection
                                    readConn.requestMethod = "POST"
                                    readConn.setRequestProperty("Content-Type", "application/json")
                                    readConn.setRequestProperty("Authorization", "Bearer $token")
                                    readConn.doOutput = true
                                    readConn.outputStream.use { it.write("{}".toByteArray()) }
                                    readConn.responseCode
                                    readConn.disconnect()
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding()
                .padding(16.dp)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部栏：工具栏 + 小兔子
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeToolBar(
                    onMenuClick = { showDrawer = true },
                    onAnnouncementClick = {
                        loadAnnouncements()
                        showAnnouncement = true
                    }
                )
                BunnyMascotLying(modifier = Modifier.size(72.dp, 50.dp))
            }

        Spacer(modifier = Modifier.height(12.dp))

        // 月亮背景 + 圆环
        Box(
            modifier = Modifier
                .size(300.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            // 月亮背景图
            Image(
                painter = painterResource(id = R.drawable.moon_bg),
                contentDescription = "月亮装饰",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // ── 流星效果 ──
            ShootingStarsEffect(modifier = Modifier.fillMaxSize())

            // ── 左上角模式切换按钮 ──
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isHealthMode) {
                            // 1. 先检查 ACTIVITY_RECOGNITION 权限
                            val hasActivityPerm = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                                androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, android.Manifest.permission.ACTIVITY_RECOGNITION
                                )
                            if (!hasActivityPerm) {
                                activityPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                            } else {
                                sensorPermReady = true
                                isHealthMode = true
                            }
                            // 2. 请求通知权限（Android 13+）
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                val hasNotifPerm = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                                    androidx.core.content.ContextCompat.checkSelfPermission(
                                        context, android.Manifest.permission.POST_NOTIFICATIONS
                                    )
                                if (!hasNotifPerm) {
                                    activityPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                // 3. 启动前台服务（常驻步数通知）
                try {
                    if (!com.laileme.app.service.KeepAliveService.isRunning(context)) {
                        com.laileme.app.service.KeepAliveService.start(context)
                    }
                } catch (_: Exception) { }
                        } else {
                            isHealthMode = false
                            showPartnerSteps = false
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                color = if (isHealthMode) Color(0xFFFF9800).copy(alpha = 0.15f) else PrimaryPink.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isHealthMode) Icons.Outlined.DirectionsRun else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isHealthMode) Color(0xFFFF9800) else PrimaryPink
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isHealthMode) "运动" else "经期",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHealthMode) Color(0xFFFF9800) else PrimaryPink
                    )
                }
            }

            // ── 右上角伴侣/我的切换按钮（仅运动模式显示，用alpha淡入避免抖动） ──
            val partnerBtnAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isHealthMode) 1f else 0f,
                animationSpec = androidx.compose.animation.core.tween(400),
                label = "partnerBtnAlpha"
            )
            if (partnerBtnAlpha > 0f) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .graphicsLayer { alpha = partnerBtnAlpha }
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = isHealthMode
                        ) {
                            showPartnerSteps = !showPartnerSteps
                            if (showPartnerSteps) {
                                healthScope.launch { fetchPartnerSteps() }
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (showPartnerSteps) Color(0xFF4FC3F7).copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showPartnerSteps) Icons.Outlined.Person else Icons.Outlined.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (showPartnerSteps) Color(0xFF4FC3F7) else Color(0xFFFF9800)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showPartnerSteps) "伴侣" else "我的",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showPartnerSteps) Color(0xFF4FC3F7) else Color(0xFFFF9800)
                        )
                    }
                }
            }

            // 缩小的圆环，偏移对准月亮内圈
            // 翻转动画：切换时Y轴翻面180°
            val ringRotation by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isHealthMode) 180f else 0f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 600,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                ),
                label = "ringRotation"
            )

            Box(
                modifier = Modifier
                    .size(115.dp)
                    .offset(x = 18.dp, y = (-41).dp)
                    .graphicsLayer {
                        rotationY = ringRotation
                        cameraDistance = 12f * density
                    },
                contentAlignment = Alignment.Center
            ) {
                // ── 根据模式显示不同圆环 ──
                if (isHealthMode) {
                    HealthRing(
                        stepsProgress = displaySteps.toFloat() / stepsGoal,
                        caloriesProgress = displayCalories.toFloat() / caloriesGoal,
                        exerciseProgress = displayExerciseTime.toFloat() / exerciseTimeGoal
                    )
                } else {
                    CycleRing(uiState)
                }

                // ── 中间文字（反向翻转补偿，防止镜像） ──
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset(y = (-4).dp)
                        .graphicsLayer {
                            rotationY = ringRotation
                        }
                ) {
                    if (isHealthMode) {
                        // 运动健康模式中间文字（淡入淡出切换）
                        androidx.compose.animation.Crossfade(
                            targetState = showPartnerSteps,
                            animationSpec = tween(300)
                        ) { isPartner ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.widthIn(min = 80.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPartner) Icons.Outlined.Favorite else Icons.Outlined.DirectionsRun,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isPartner) Color(0xFF4FC3F7) else Color(0xFFFF9800)
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    if (isPartner) partnerNickname else "步数",
                                    fontSize = 8.sp, color = TextHint,
                                    textAlign = TextAlign.Center
                                )
                                if (isLoadingPartner && isPartner) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF4FC3F7)
                                    )
                                } else {
                                    Text(
                                        "${if (isPartner) partnerSteps else currentSteps}",
                                        fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                        color = if (isPartner) Color(0xFF4FC3F7) else Color(0xFFFF9800),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Text("/ $stepsGoal", fontSize = 8.sp, color = TextHint, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        // 原有经期模式文字
                        val ringGender = AuthManager.userState.collectAsState().value?.gender ?: "female"
                        if (uiState.latestRecord != null) {
                            val cycleLen = uiState.latestRecord?.cycleLength ?: 28
                            val phase = (uiState.cycleDay.toFloat() / cycleLen).coerceIn(0f, 1f)
                            // 根据周期阶段选择月亮icon
                            val moonIcon = when {
                                phase < 0.125f -> Icons.Outlined.DarkMode       // 新月
                                phase < 0.375f -> Icons.Outlined.NightsStay     // 上弦
                                phase < 0.625f -> Icons.Outlined.LightMode      // 满月
                                phase < 0.875f -> Icons.Outlined.NightsStay     // 下弦
                                else -> Icons.Outlined.DarkMode                  // 残月
                            }
                            val moonTint = when {
                                phase < 0.375f -> Color(0xFF7986CB)
                                phase < 0.625f -> Color(0xFFFDD835)
                                else -> Color(0xFF7986CB)
                            }
                            Icon(
                                imageVector = moonIcon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = moonTint
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                        }
                        if (ringGender == "male") {
                            if (hasRecord) {
                                Text("伴侣", fontSize = 8.sp, color = TextHint)
                                if (uiState.isFirstRecord && uiState.isInPeriod) {
                                    Text("经期中", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PeriodRed)
                                    Text("第${uiState.cycleDay}天", fontSize = 10.sp, color = TextSecondary)
                                } else if (uiState.isFirstRecord && !uiState.isInPeriod) {
                                    Text("收集中", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryPink)
                                    Text("等待下次记录", fontSize = 10.sp, color = TextSecondary)
                                } else if (uiState.isInPeriod) {
                                    Text("${uiState.daysUntilPeriodEnd}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PeriodRed)
                                    Text("天后结束", fontSize = 10.sp, color = TextSecondary)
                                    Text("周期第 ${uiState.cycleDay} 天", fontSize = 8.sp, color = TextHint)
                                } else {
                                    Text("${uiState.daysUntilNextPeriod}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4FC3F7))
                                    Text("天后来", fontSize = 10.sp, color = TextSecondary)
                                    Text("周期第 ${uiState.cycleDay} 天", fontSize = 8.sp, color = TextHint)
                                }
                            } else {
                                Text("♡", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4FC3F7))
                                Text("伴侣模式", fontSize = 10.sp, color = TextSecondary)
                            }
                        } else {
                            if (hasRecord) {
                                if (uiState.isFirstRecord && uiState.isInPeriod) {
                                    Text("经期", fontSize = 8.sp, color = TextHint)
                                    Text("记录中", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PeriodRed)
                                    Text("第${uiState.cycleDay}天", fontSize = 10.sp, color = TextSecondary)
                                } else if (uiState.isFirstRecord && !uiState.isInPeriod) {
                                    Text("周期", fontSize = 8.sp, color = TextHint)
                                    Text("收集中", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryPink)
                                    Text("等待下次记录", fontSize = 10.sp, color = TextSecondary)
                                } else if (uiState.isInPeriod) {
                                    Text("预计", fontSize = 8.sp, color = TextHint)
                                    Text("${uiState.daysUntilPeriodEnd}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PeriodRed)
                                    Text("天后结束", fontSize = 10.sp, color = TextSecondary)
                                    Text("周期第 ${uiState.cycleDay} 天", fontSize = 8.sp, color = TextHint)
                                } else {
                                    Text("预计", fontSize = 8.sp, color = TextHint)
                                    Text("${uiState.daysUntilNextPeriod}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PrimaryPink)
                                    Text("天后来", fontSize = 10.sp, color = TextSecondary)
                                    Text("周期第 ${uiState.cycleDay} 天", fontSize = 8.sp, color = TextHint)
                                }
                            } else {
                                Text("—", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextHint)
                                Text("记录经期", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 状态卡片：统一渲染，根据模式切换数据（避免if/else重建导致抖动）
        val completedRecords = uiState.records.filter { it.endDate != null }
        val latestCompleted = completedRecords.firstOrNull()
        val card1Title = if (isHealthMode) (if (showPartnerSteps) "${partnerNickname}步数" else "步数") else "周期"
        val card1Value = if (isHealthMode) "$displaySteps" else (if (latestCompleted != null && !uiState.isFirstRecord) "${latestCompleted.cycleLength}" else "--")
        val card1Unit = if (isHealthMode) "步" else "天"
        val card1Color = if (isHealthMode) (if (showPartnerSteps) Color(0xFF4FC3F7) else Color(0xFFFF9800)) else AccentTeal
        val card2Title = if (isHealthMode) "能量" else "经期"
        val card2Value = if (isHealthMode) "$displayCalories" else (if (latestCompleted != null) "${latestCompleted.periodLength}" else "--")
        val card2Unit = if (isHealthMode) "kcal" else "天"
        val card2Color = if (isHealthMode) Color(0xFFFFC107) else PrimaryPink
        val card3Title = if (isHealthMode) "运动" else "记录"
        val card3Value = if (isHealthMode) "$displayExerciseTime" else (if (completedRecords.isNotEmpty()) "${completedRecords.size}" else "--")
        val card3Unit = if (isHealthMode) "分钟" else "次"
        val card3Color = if (isHealthMode) Color(0xFF42A5F5) else AccentOrange
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatusCard(card1Title, card1Value, card1Unit, card1Color, modifier = Modifier.weight(1f))
            StatusCard(card2Title, card2Value, card2Unit, card2Color, modifier = Modifier.weight(1f))
            StatusCard(card3Title, card3Value, card3Unit, card3Color, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 小贴士（随机轮播）- 运动模式下从左往右滑出收起
        var tipIndex by remember { mutableIntStateOf((Math.random() * periodTips.size).toInt()) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(6000)
                tipIndex = (tipIndex + (1..periodTips.size - 1).random()) % periodTips.size
            }
        }
        AnimatedVisibility(
            visible = !isHealthMode,
            enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(300))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = LightPink),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LightbulbCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = PrimaryPink
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    AnimatedContent(
                        targetState = tipIndex,
                        transitionSpec = {
                            (fadeIn() + slideInVertically { it }) togetherWith
                                    (fadeOut() + slideOutVertically { -it })
                        },
                        label = "tip"
                    ) { index ->
                        Text(
                            text = periodTips[index],
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── 运动详情卡片（运动模式下，等月经按钮收起后延迟弹出） ──
        var showHealthCards by remember { mutableStateOf(false) }
        // 月经按钮显示状态（等运动卡片收起后再弹出）
        var showPeriodButtons by remember { mutableStateOf(!isHealthMode) }
        LaunchedEffect(isHealthMode) {
            if (isHealthMode) {
                // 先让月经按钮收起，等收起完再弹运动卡片
                showPeriodButtons = false
                delay(450)
                showHealthCards = true
            } else {
                // 先收起运动卡片，等收起完再弹月经按钮
                showHealthCards = false
                delay(400)
                showPeriodButtons = true
            }
        }
        AnimatedVisibility(
            visible = showHealthCards,
            enter = expandVertically(expandFrom = Alignment.Top, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HealthDetailCard(
                    icon = Icons.Outlined.LocalFireDepartment,
                    title = if (showPartnerSteps) "${partnerNickname}消耗" else "消耗",
                    value = "$displayCalories",
                    unit = "千卡",
                    subtitle = "≈ ${String.format("%.1f", displayCalories / 100f)} 个香蕉",
                    color = Color(0xFFFF9800),
                    onClick = { showExerciseStats = true }
                )
                HealthDetailCard(
                    icon = Icons.Outlined.DirectionsWalk,
                    title = if (showPartnerSteps) "${partnerNickname}步数" else "步数",
                    value = "$displaySteps",
                    unit = "步",
                    subtitle = if (displaySteps < 3000) "小走几步，身体更健康" else if (displaySteps < 8000) "继续加油，快达标了！" else "太棒了，今日达标！",
                    color = if (showPartnerSteps) Color(0xFF4FC3F7) else Color(0xFFFF5722),
                    onClick = { showExerciseStats = true }
                )
                HealthDetailCard(
                    icon = Icons.Outlined.FitnessCenter,
                    title = if (showPartnerSteps) "${partnerNickname}锻炼" else "锻炼时长",
                    value = "$displayExerciseTime",
                    unit = "分钟",
                    subtitle = if (displayExerciseTime < 15) "坚持运动！离目标更近" else if (displayExerciseTime < 45) "运动中，保持节奏！" else "运动达人！继续保持",
                    color = Color(0xFF42A5F5),
                    onClick = { showExerciseStats = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 操作按钮 - 仅女性用户显示，运动健康模式下收起（等运动卡片收完再弹）
        val userGender = AuthManager.userState.collectAsState().value?.gender ?: "female"
        AnimatedVisibility(
            visible = showPeriodButtons && userGender == "female",
            enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
            exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(animationSpec = tween(300))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HomeIconActionButton(
                        icon = Icons.Outlined.PlayCircleOutline,
                        label = "月经开始",
                        color = if (!uiState.hasActivePeriod) PrimaryPink else TextHint,
                        onClick = {
                            if (!uiState.hasActivePeriod) {
                                if (!uiState.hasSetup) {
                                    showModeDialog = true
                                } else {
                                    onAddPeriod(normalizeDate(System.currentTimeMillis()))
                                }
                            }
                        }
                    )
                    HomeIconActionButton(
                        icon = Icons.Outlined.EditCalendar,
                        label = "月经已来",
                        color = if (!uiState.hasActivePeriod) AccentOrange else TextHint,
                        onClick = {
                            if (!uiState.hasActivePeriod) {
                                if (!uiState.hasSetup) {
                                    showModeDialog = true
                                } else {
                                    alreadyDaysInput = ""
                                    showAlreadyStartedDialog = true
                                }
                            }
                        }
                    )
                    HomeIconActionButton(
                        icon = Icons.Outlined.StopCircle,
                        label = "月经结束",
                        color = if (uiState.hasActivePeriod) PeriodRed else TextHint,
                        onClick = {
                            if (uiState.hasActivePeriod) onEndPeriod(normalizeDate(System.currentTimeMillis()))
                        }
                    )
                    HomeIconActionButton(
                        icon = Icons.Outlined.Refresh,
                        label = "重置",
                        color = if (uiState.latestRecord != null) AccentOrange else TextHint,
                        onClick = {
                            if (uiState.latestRecord != null) showResetConfirm = true
                        }
                    )
                }
            }
        }
        // ── 男性用户：关怀按钮（每天一次，运动模式下也隐藏）──
        AnimatedVisibility(
            visible = !isHealthMode && userGender == "male",
            enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
            exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(animationSpec = tween(300))
        ) {
            val careScope = rememberCoroutineScope()
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = !isSendingCare && !hasCaredToday) {
                        isSendingCare = true
                        careScope.launch {
                            val token = context.getSharedPreferences("laileme_auth", Context.MODE_PRIVATE)
                                .getString("token", "") ?: ""
                            val (ok, _) = PartnerManager.sendCareMessage(token, "care")
                            isSendingCare = false
                            if (ok) {
                                carePrefs.edit().putString("last_care_date", todayStr).apply()
                                hasCaredToday = true
                            } else {
                                android.widget.Toast.makeText(context, "发送失败，请检查网络", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                color = if (hasCaredToday) AccentTeal.copy(alpha = 0.08f) else Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isSendingCare) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = PrimaryPink
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("发送中...", fontSize = 15.sp, color = TextHint)
                    } else if (hasCaredToday) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = AccentTeal
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "今日已关怀哦",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentTeal
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = PrimaryPink
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "发送关怀",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryPink
                        )
                    }
                }
            }
        }
    }

    // 首次使用 - 模式选择对话框
    if (showModeDialog) {
        ModeSelectionDialog(
            onDismiss = { showModeDialog = false },
            onSelectAuto = {
                // 选择自动推算模式：保存模式，直接开始记录
                onSaveMode("auto")
                onAddPeriod(normalizeDate(System.currentTimeMillis()))
                showModeDialog = false
            },
            onSelectManual = {
                // 选择手动输入模式：先保存模式，再弹出周期设置
                onSaveMode("manual")
                showModeDialog = false
                showManualSetupDialog = true
            }
        )
    }

    // 手动模式 - 输入周期设置对话框
    if (showManualSetupDialog) {
        ManualSetupDialog(
            onDismiss = { showManualSetupDialog = false },
            onConfirm = { cycle, period ->
                onSaveSettings(cycle, period)
                onAddPeriod(normalizeDate(System.currentTimeMillis()))
                showManualSetupDialog = false
            }
        )
    }

    // 月经已来弹窗 — 补录忘记记录的经期
    if (showAlreadyStartedDialog) {
        AlertDialog(
            onDismissRequest = { showAlreadyStartedDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            icon = {
                Icon(Icons.Outlined.EditCalendar, null, tint = AccentOrange, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("月经已来", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "忘记记录了？输入月经已经来了几天\n（包含今天）",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("已来 ", fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = alreadyDaysInput,
                            onValueChange = { v ->
                                alreadyDaysInput = v.filter { it.isDigit() }.take(2)
                            },
                            modifier = Modifier.width(60.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            shape = RoundedCornerShape(10.dp),
                            placeholder = { Text("", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = TextHint) }
                        )
                        Text(" 天", fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                    val days = alreadyDaysInput.toIntOrNull() ?: 0
                    if (days > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val startDate = java.util.Calendar.getInstance().apply {
                            add(java.util.Calendar.DAY_OF_YEAR, -(days - 1))
                        }
                        val fmt = java.text.SimpleDateFormat("M月d日", java.util.Locale.getDefault())
                        Text(
                            "将记录经期从 ${fmt.format(startDate.time)} 开始",
                            fontSize = 12.sp,
                            color = AccentOrange,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val days = alreadyDaysInput.toIntOrNull() ?: 0
                        if (days > 0) {
                            val startMs = normalizeDate(System.currentTimeMillis()) - (days - 1) * 24L * 60 * 60 * 1000
                            onAddPeriod(startMs)
                            showAlreadyStartedDialog = false
                        }
                    },
                    enabled = (alreadyDaysInput.toIntOrNull() ?: 0) > 0
                ) {
                    Text("确认", fontWeight = FontWeight.Bold, color = if ((alreadyDaysInput.toIntOrNull() ?: 0) > 0) PrimaryPink else TextHint)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAlreadyStartedDialog = false }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }

    // 重置确认弹窗
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    "确认重置",
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange
                )
            },
            text = {
                Text(
                    "将删除当前周期记录，历史已完成的记录不受影响。确定要重置吗？",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReset()
                        showResetConfirm = false
                    }
                ) {
                    Text("确认重置", color = AccentOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("取消", color = TextHint)
                }
            }
        )
    }

        // ── 公告弹窗（自定义动画） ──
        AnimatedVisibility(
            visible = showAnnouncement,
            enter = fadeIn(tween(200)) + scaleIn(
                initialScale = 0.85f,
                animationSpec = tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ),
            exit = fadeOut(tween(150)) + scaleOut(
                targetScale = 0.85f,
                animationSpec = tween(150)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showAnnouncement = false },
                contentAlignment = Alignment.Center
            ) {
                // 兔子 + 公告卡片整体
                Column(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 兔子吉祥物（底部与卡片重叠一点）
                    BunnyMascot(modifier = Modifier
                        .size(56.dp, 68.dp)
                        .offset(y = 10.dp)
                    )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {},
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // 标题栏
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Campaign,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = PrimaryPink
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("官方公告", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                            }
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "关闭",
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .clickable { showAnnouncement = false },
                                tint = TextHint
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        Spacer(modifier = Modifier.height(12.dp))

                        // 内容区域（固定最小高度避免跳变）
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 350.dp),
                            contentAlignment = if (isLoadingAnnouncements) Alignment.Center else Alignment.TopStart
                        ) {
                            if (isLoadingAnnouncements) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = PrimaryPink,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                ) {
                                    announcements.forEachIndexed { index, item ->
                                        if (index > 0) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                color = Color(0xFFF0F0F0)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp,
                                                color = TextPrimary
                                            )
                                            if (item.time.isNotEmpty()) {
                                                Text(
                                                    text = item.time,
                                                    fontSize = 11.sp,
                                                    color = TextHint,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = item.content,
                                                fontSize = 13.sp,
                                                color = TextSecondary,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 底部按钮
                        TextButton(
                            onClick = { showAnnouncement = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("知道了", color = PrimaryPink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
                } // end Column(兔子+卡片)
            }
        }

        // ── 收到关怀弹窗（女方）──
        AnimatedVisibility(
            visible = showCareReceived,
            enter = fadeIn(tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f, animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showCareReceived = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {},
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = PrimaryPink
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "收到一份关怀",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPink
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${careFromName} 向你发送了关怀",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "TA 在想你哦～",
                            fontSize = 13.sp,
                            color = TextHint
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        TextButton(onClick = { showCareReceived = false }) {
                            Text(
                                "收到啦",
                                color = PrimaryPink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // ── 快捷设置侧边栏 ──
        QuickSettingsDrawer(
            visible = showDrawer,
            onDismiss = { showDrawer = false },
            uiState = uiState,
            onSaveSettings = onSaveSettings,
            onSaveMode = onSaveMode
        )

        // ── 运动统计详情页（全屏覆盖层） ──
        AnimatedVisibility(
            visible = showExerciseStats,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(tween(300)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
            ) {
                ExerciseStatsScreen(
                    onBack = { showExerciseStats = false },
                    currentUserId = currentUserId
                )
            }
        }
    } // end Box
}

@Composable
private fun HomeIconActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun ModeSelectionDialog(
    onDismiss: () -> Unit,
    onSelectAuto: () -> Unit,
    onSelectManual: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                "选择记录模式",
                color = PrimaryPink, fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
        },
        text = {
            Column {
                Text(
                    "欢迎使用来了么~ 请选择适合你的记录方式：",
                    color = TextSecondary, fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 自动推算模式
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelectAuto),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryPink.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = PrimaryPink
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "先记录，自动推算",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPink
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "适合不清楚自己周期的用户。每次记录经期的开始和结束，系统会根据你的历史数据自动计算周期长度和经期天数，越用越准哦~",
                            fontSize = 11.sp, color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "使用步骤：点击开始 → 经期结束时点结束 → 下次点开始时系统就能算出你的周期啦",
                            fontSize = 10.sp, color = PrimaryPink.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 手动输入模式
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelectManual),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentTeal.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.EditCalendar,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = AccentTeal
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "手动输入周期",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentTeal
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "适合已经了解自己周期的用户。直接输入你的月经周期天数和经期天数，系统将使用固定值来预测。",
                            fontSize = 11.sp, color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 温馨提示卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = AccentOrange
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "温馨提示",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "• 选择「自动推算」后，第一次记录时首页圆环显示为 0 是正常的哦，因为还没有历史数据来计算周期~\n• 完成第一次完整记录（开始→结束）后，第二次点击开始时系统就会自动推算出周期啦！\n• 后续可在「我的」页面随时修改记录模式和周期设置~",
                            fontSize = 10.sp, color = TextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("暂不设置", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun ManualSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var cycleLength by remember { mutableStateOf("28") }
    var periodLength by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                "设置周期参数",
                color = PrimaryPink, fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
        },
        text = {
            Column {
                Text(
                    "请输入你的月经周期信息：",
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
                Text("确定并记录", color = PrimaryPink, fontWeight = FontWeight.Bold)
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
private fun CycleRing(uiState: PeriodUiState) {
    val cycleLength = uiState.latestRecord?.cycleLength ?: 28
    val periodLength = uiState.latestRecord?.periodLength ?: 5
    val cycleDay = uiState.cycleDay
    val isInPeriod = uiState.isInPeriod
    val hasRecord = uiState.latestRecord != null

    // ── 外圈：经期进度（经期中显示经期天数/经期长度，非经期显示0） ──
    val outerTargetSweep = if (hasRecord && isInPeriod && cycleDay > 0) {
        val raw = cycleDay.toFloat() / periodLength
        (raw.coerceAtMost(0.95f)) * 360f
    } else 0f

    // ── 内圈：周期进度（仅在周期数据有效时显示） ──
    val hasCycleData = hasRecord && !uiState.isFirstRecord
    val innerTargetSweep = if (hasCycleData && cycleDay > 0) {
        val raw = cycleDay.toFloat() / cycleLength
        (raw.coerceAtMost(0.95f)) * 360f
    } else 0f

    // 动画
    val animatedOuterSweep by androidx.compose.animation.core.animateFloatAsState(
        targetValue = outerTargetSweep.coerceAtLeast(if (hasRecord && isInPeriod && cycleDay > 0) 1f else 0f),
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 800,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "outerRingProgress"
    )

    val animatedInnerSweep by androidx.compose.animation.core.animateFloatAsState(
        targetValue = innerTargetSweep.coerceAtLeast(if (hasCycleData && cycleDay > 0) 1f else 0f),
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 1000,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "innerRingProgress"
    )

    val outerColor = PeriodRed          // 外圈：经期红
    val innerColor = AccentTeal         // 内圈：周期绿（和底部周期文字一致）

    if (!hasRecord) {
        // 无记录时不显示圆环背板和进度
        return
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val outerStrokeWidth = 8.dp.toPx()
        val innerStrokeWidth = 6.dp.toPx()
        val gap = 2.dp.toPx()
        val outerRadius = (size.minDimension - outerStrokeWidth) / 2
        val innerRadius = outerRadius - outerStrokeWidth / 2 - gap - innerStrokeWidth / 2
        val center = Offset(size.width / 2, size.height / 2)

        // ── 外圈背景 ──
        drawCircle(
            color = Color(0xFFF0F0F0).copy(alpha = 0.6f),
            radius = outerRadius, center = center,
            style = Stroke(width = outerStrokeWidth, cap = StrokeCap.Round)
        )

        // ── 内圈背景 ──
        drawCircle(
            color = Color(0xFFF0F0F0).copy(alpha = 0.4f),
            radius = innerRadius, center = center,
            style = Stroke(width = innerStrokeWidth, cap = StrokeCap.Round)
        )

        // ── 外圈进度弧（经期） ──
        if (animatedOuterSweep > 0f) {
            drawArc(
                color = outerColor,
                startAngle = -90f,
                sweepAngle = animatedOuterSweep,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = outerStrokeWidth, cap = StrokeCap.Round)
            )
            // 外圈进度点
            val angle = (animatedOuterSweep - 90f) * (Math.PI / 180.0)
            val ix = center.x + outerRadius * cos(angle).toFloat()
            val iy = center.y + outerRadius * sin(angle).toFloat()
            drawCircle(Color.White, 6.dp.toPx(), Offset(ix, iy))
            drawCircle(outerColor, 4.dp.toPx(), Offset(ix, iy))
        }

        // ── 内圈进度弧（周期） ──
        if (animatedInnerSweep > 0f) {
            drawArc(
                color = innerColor,
                startAngle = -90f,
                sweepAngle = animatedInnerSweep,
                useCenter = false,
                topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                size = Size(innerRadius * 2, innerRadius * 2),
                style = Stroke(width = innerStrokeWidth, cap = StrokeCap.Round)
            )
            // 内圈进度点
            val angle2 = (animatedInnerSweep - 90f) * (Math.PI / 180.0)
            val ix2 = center.x + innerRadius * cos(angle2).toFloat()
            val iy2 = center.y + innerRadius * sin(angle2).toFloat()
            drawCircle(Color.White, 5.dp.toPx(), Offset(ix2, iy2))
            drawCircle(innerColor, 3.dp.toPx(), Offset(ix2, iy2))
        }
    }
}

// ══════════════════════════════════════════
// 运动健康三层圆环
// ══════════════════════════════════════════
@Composable
private fun HealthRing(
    stepsProgress: Float,
    caloriesProgress: Float,
    exerciseProgress: Float
) {
    val stepsColor = Color(0xFFFF9800)     // 橙色 - 步数（最外圈）
    val caloriesColor = Color(0xFFFFC107)  // 黄色 - 能量（中间）
    val exerciseColor = Color(0xFF42A5F5)  // 蓝色 - 运动时间（最内圈）

    val animSteps by androidx.compose.animation.core.animateFloatAsState(
        targetValue = (stepsProgress.coerceIn(0f, 0.95f)) * 360f,
        animationSpec = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "stepsRing"
    )
    val animCalories by androidx.compose.animation.core.animateFloatAsState(
        targetValue = (caloriesProgress.coerceIn(0f, 0.95f)) * 360f,
        animationSpec = androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "caloriesRing"
    )
    val animExercise by androidx.compose.animation.core.animateFloatAsState(
        targetValue = (exerciseProgress.coerceIn(0f, 0.95f)) * 360f,
        animationSpec = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "exerciseRing"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeOuter = 7.dp.toPx()
        val strokeMiddle = 6.dp.toPx()
        val strokeInner = 5.dp.toPx()
        val gap = 2.dp.toPx()

        val outerRadius = (size.minDimension - strokeOuter) / 2
        val middleRadius = outerRadius - strokeOuter / 2 - gap - strokeMiddle / 2
        val innerRadius = middleRadius - strokeMiddle / 2 - gap - strokeInner / 2
        val center = Offset(size.width / 2, size.height / 2)

        // ── 外圈背景（步数） ──
        drawCircle(color = stepsColor.copy(alpha = 0.15f), radius = outerRadius, center = center,
            style = Stroke(width = strokeOuter, cap = StrokeCap.Round))
        // ── 中圈背景（能量） ──
        drawCircle(color = caloriesColor.copy(alpha = 0.15f), radius = middleRadius, center = center,
            style = Stroke(width = strokeMiddle, cap = StrokeCap.Round))
        // ── 内圈背景（运动） ──
        drawCircle(color = exerciseColor.copy(alpha = 0.15f), radius = innerRadius, center = center,
            style = Stroke(width = strokeInner, cap = StrokeCap.Round))

        // ── 外圈进度（步数） ──
        if (animSteps > 0f) {
            drawArc(color = stepsColor, startAngle = -90f, sweepAngle = animSteps, useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = strokeOuter, cap = StrokeCap.Round))
            val a1 = (animSteps - 90f) * (Math.PI / 180.0)
            drawCircle(Color.White, 5.dp.toPx(), Offset(center.x + outerRadius * cos(a1).toFloat(), center.y + outerRadius * sin(a1).toFloat()))
            drawCircle(stepsColor, 3.dp.toPx(), Offset(center.x + outerRadius * cos(a1).toFloat(), center.y + outerRadius * sin(a1).toFloat()))
        }

        // ── 中圈进度（能量） ──
        if (animCalories > 0f) {
            drawArc(color = caloriesColor, startAngle = -90f, sweepAngle = animCalories, useCenter = false,
                topLeft = Offset(center.x - middleRadius, center.y - middleRadius),
                size = Size(middleRadius * 2, middleRadius * 2),
                style = Stroke(width = strokeMiddle, cap = StrokeCap.Round))
            val a2 = (animCalories - 90f) * (Math.PI / 180.0)
            drawCircle(Color.White, 4.dp.toPx(), Offset(center.x + middleRadius * cos(a2).toFloat(), center.y + middleRadius * sin(a2).toFloat()))
            drawCircle(caloriesColor, 2.5.dp.toPx(), Offset(center.x + middleRadius * cos(a2).toFloat(), center.y + middleRadius * sin(a2).toFloat()))
        }

        // ── 内圈进度（运动时间） ──
        if (animExercise > 0f) {
            drawArc(color = exerciseColor, startAngle = -90f, sweepAngle = animExercise, useCenter = false,
                topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                size = Size(innerRadius * 2, innerRadius * 2),
                style = Stroke(width = strokeInner, cap = StrokeCap.Round))
            val a3 = (animExercise - 90f) * (Math.PI / 180.0)
            drawCircle(Color.White, 4.dp.toPx(), Offset(center.x + innerRadius * cos(a3).toFloat(), center.y + innerRadius * sin(a3).toFloat()))
            drawCircle(exerciseColor, 2.5.dp.toPx(), Offset(center.x + innerRadius * cos(a3).toFloat(), center.y + innerRadius * sin(a3).toFloat()))
        }
    }
}

// ══════════════════════════════════════════
// 运动详情卡片
// ══════════════════════════════════════════
@Composable
private fun HealthDetailCard(
    icon: ImageVector,
    title: String,
    value: String,
    unit: String,
    subtitle: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // 右侧信息（文字淡入淡出）
            Column(modifier = Modifier.weight(1f)) {
                androidx.compose.animation.Crossfade(targetState = title, animationSpec = tween(250)) { t ->
                    Text(t, fontSize = 13.sp, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.animation.Crossfade(targetState = value to unit, animationSpec = tween(250)) { (v, u) ->
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(v, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(u, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.offset(y = (-3).dp))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                androidx.compose.animation.Crossfade(targetState = subtitle, animationSpec = tween(250)) { s ->
                    Text(s, fontSize = 11.sp, color = TextHint, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun HomeToolBar(onMenuClick: () -> Unit = {}, onAnnouncementClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier.padding(top = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeToolBarIcon(Icons.Outlined.Menu, false, onClick = onMenuClick)
            HomeToolBarIcon(Icons.Outlined.Campaign, false, onClick = onAnnouncementClick)
            HomeToolBarIcon(Icons.Outlined.DarkMode, false)
            HomeToolBarIcon(Icons.Outlined.ChatBubbleOutline, false)
        }
    }
}

@Composable
private fun HomeToolBarIcon(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AccentOrange.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isSelected) AccentOrange else TextSecondary
        )
    }
}

@Composable
private fun StatusCard(title: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.animation.Crossfade(targetState = title, animationSpec = tween(250)) { t ->
                var fs by remember(t) { mutableStateOf(10f) }
                Text(
                    t,
                    fontSize = fs.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    onTextLayout = { result ->
                        if (result.hasVisualOverflow && fs > 6f) {
                            fs -= 0.5f
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            // 数值淡入淡出
            androidx.compose.animation.Crossfade(targetState = "$value|$unit" to color, animationSpec = tween(250)) { (vu, c) ->
                val parts = vu.split("|")
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text(parts[0], fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c)
                    Text(parts.getOrElse(1) { "" }, fontSize = 9.sp, color = TextHint, modifier = Modifier.padding(bottom = 1.dp))
                }
            }
        }
    }
}

// ══════════════════════════════════════════
// 快捷设置侧边栏
// ══════════════════════════════════════════
@Composable
private fun QuickSettingsDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    uiState: PeriodUiState,
    onSaveSettings: (Int, Int) -> Unit,
    onSaveMode: (String) -> Unit
) {
    val context = LocalContext.current
    val user by AuthManager.userState.collectAsState()
    val isLoggedIn = user != null

    // 密码锁状态
    val settingsPrefs = remember { context.getSharedPreferences("laileme_settings", Context.MODE_PRIVATE) }
    var passwordEnabled by remember { mutableStateOf(settingsPrefs.getString("app_password", "")?.isNotEmpty() == true) }
    var notificationEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("period_notification", true)) }

    // 弹窗状态
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }
    var showCycleDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    // 背景遮罩
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(250))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss
                )
        )
    }

    // 侧边栏面板
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(300)
        ) + fadeIn(tween(300)),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(250)
        ) + fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.72f)
                .background(Color.White)
                .statusBarsPadding()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {} // 阻止点击穿透
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // ── 用户信息区 ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    if (isLoggedIn) {
                        val avatarUrl = user?.avatarUrl ?: ""
                        val fullAvatarUrl = if (avatarUrl.isNotBlank() && !avatarUrl.startsWith("http"))
                            "http://47.123.5.171:8080$avatarUrl" else avatarUrl

                        if (fullAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = fullAvatarUrl,
                                contentDescription = "头像",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryPink.copy(alpha = 0.1f)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryPink.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = PrimaryPink
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    user?.nickname ?: "用户",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (PaymentManager.isLocalVip(context)) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFFD700))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("VIP", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF8B4513))
                                    }
                                }
                            }
                            if (user?.uid?.isNotEmpty() == true) {
                                Text(
                                    "UID: ${user?.uid}",
                                    fontSize = 11.sp,
                                    color = TextHint
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF0F0F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = TextHint
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "请登录",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextHint
                        )
                    }
                }

                Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // ── 快捷设置项 ──
                Text(
                    "快捷设置",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPink,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 提醒设置
                DrawerSettingItem(
                    icon = Icons.Outlined.Notifications,
                    title = "提醒设置",
                    subtitle = if (notificationEnabled) "已开启" else "已关闭",
                    onClick = { showNotificationDialog = true }
                )

                // 权限管理
                DrawerSettingItem(
                    icon = Icons.Outlined.Security,
                    title = "权限管理",
                    subtitle = "查看和管理应用权限",
                    onClick = { showPermissionDialog = true }
                )

                // 记录模式（男性隐藏）
                val currentMode = uiState.trackingMode
                val drawerGender = AuthManager.userState.collectAsState().value?.gender ?: "female"
                if (drawerGender != "male") {
                    DrawerSettingItem(
                        icon = Icons.Outlined.SwapHoriz,
                        title = "记录模式",
                        subtitle = if (currentMode == "auto") "自动推算" else "手动记录",
                        onClick = { showModeDialog = true }
                    )

                    // 周期设置
                    DrawerSettingItem(
                        icon = Icons.Outlined.DateRange,
                        title = "周期设置",
                        subtitle = "周期${uiState.savedCycleLength}天 · 经期${uiState.savedPeriodLength}天",
                        onClick = { showCycleDialog = true }
                    )
                }

                // 密码锁
                DrawerSettingItem(
                    icon = Icons.Outlined.Lock,
                    title = "密码锁",
                    subtitle = if (passwordEnabled) "已开启" else "未设置",
                    onClick = { showPasswordDialog = true }
                )
            }
        }
    }

    // ── 提醒设置弹窗 ──
    var stepNotificationEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("step_notification", true)) }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = PrimaryPink
                )
            },
            title = {
                Text("提醒设置", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    // 经期提醒开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("经期提醒", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("经期临近时推送通知", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = notificationEnabled,
                            onCheckedChange = {
                                notificationEnabled = it
                                settingsPrefs.edit().putBoolean("period_notification", it).apply()
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryPink)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // 步数通知开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("步数通知", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("通知栏显示今日步数", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = stepNotificationEnabled,
                            onCheckedChange = {
                                stepNotificationEnabled = it
                                settingsPrefs.edit().putBoolean("step_notification", it).apply()
                                try {
                                    if (it) {
                                        com.laileme.app.service.KeepAliveService.start(context)
                                    } else {
                                        com.laileme.app.service.KeepAliveService.stop(context)
                                    }
                                } catch (_: Exception) { }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFFFF9800))
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("完成", color = PrimaryPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {}
        )
    }

    // ── 权限管理弹窗 ──
    if (showPermissionDialog) {
        val permContext = context
        val hasActivityRecog = android.content.pm.PackageManager.PERMISSION_GRANTED ==
            androidx.core.content.ContextCompat.checkSelfPermission(permContext, android.Manifest.permission.ACTIVITY_RECOGNITION)
        val hasNotification = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.content.pm.PackageManager.PERMISSION_GRANTED ==
                androidx.core.content.ContextCompat.checkSelfPermission(permContext, android.Manifest.permission.POST_NOTIFICATIONS)
        } else true
        val hasBatterySaver = run {
            val pm = permContext.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
            pm?.isIgnoringBatteryOptimizations(permContext.packageName) ?: true
        }
        val hasBodySensors = android.content.pm.PackageManager.PERMISSION_GRANTED ==
            androidx.core.content.ContextCompat.checkSelfPermission(permContext, android.Manifest.permission.BODY_SENSORS)

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
                    PermissionRow("运动识别", "记录步数和运动数据", hasActivityRecog)
                    PermissionRow("通知权限", "显示提醒和步数通知", hasNotification)
                    PermissionRow("电池优化", "后台持续记录步数", hasBatterySaver)
                    PermissionRow("身体传感器", "读取计步传感器数据", hasBodySensors)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // 跳转到应用设置页面
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.parse("package:${permContext.packageName}")
                        }
                        permContext.startActivity(intent)
                    } catch (_: Exception) { }
                    showPermissionDialog = false
                }) {
                    Text("去设置", color = AccentTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("关闭", color = TextHint)
                }
            }
        )
    }

    // ── 记录模式弹窗 ──
    if (showModeDialog) {
        val currentMode = uiState.trackingMode
        AlertDialog(
            onDismissRequest = { showModeDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            icon = {
                Icon(
                    Icons.Outlined.SwapHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = AccentTeal
                )
            },
            title = {
                Text("切换记录模式", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        "当前模式：${if (currentMode == "auto") "自动推算" else "手动记录"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (currentMode == "auto") "切换到「手动记录」后，需要您手动设置周期和经期天数，系统将按您设定的数值预测。"
                        else "切换到「自动推算」后，系统将根据您的历史记录自动计算周期长度。",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newMode = if (currentMode == "auto") "manual" else "auto"
                    onSaveMode(newMode)
                    showModeDialog = false
                }) {
                    Text(
                        "切换到${if (currentMode == "auto") "手动记录" else "自动推算"}",
                        color = AccentTeal,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showModeDialog = false }) {
                    Text("取消", color = TextHint)
                }
            }
        )
    }

    // ── 周期设置弹窗 ──
    if (showCycleDialog) {
        ManualSetupDialog(
            onDismiss = { showCycleDialog = false },
            onConfirm = { cycle, period ->
                onSaveSettings(cycle, period)
                showCycleDialog = false
            }
        )
    }

    // ── 密码锁弹窗 ──
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                passwordInput = ""
                passwordConfirm = ""
                passwordError = ""
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            icon = {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (passwordEnabled) AccentOrange else PrimaryPink
                )
            },
            title = {
                Text(
                    if (passwordEnabled) "关闭密码锁" else "设置密码锁",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    if (passwordEnabled) {
                        Text(
                            "关闭密码锁后，打开应用将不再需要输入密码。",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it; passwordError = "" },
                            label = { Text("请输入当前密码", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = passwordError.isNotEmpty(),
                            supportingText = if (passwordError.isNotEmpty()) {{ Text(passwordError, color = Color.Red, fontSize = 11.sp) }} else null
                        )
                    } else {
                        Text(
                            "设置密码锁后，每次打开应用需要输入密码才能进入。",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it; passwordError = "" },
                            label = { Text("设置密码（4-6位数字）", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = passwordError.isNotEmpty()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = passwordConfirm,
                            onValueChange = { passwordConfirm = it; passwordError = "" },
                            label = { Text("确认密码", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = passwordError.isNotEmpty(),
                            supportingText = if (passwordError.isNotEmpty()) {{ Text(passwordError, color = Color.Red, fontSize = 11.sp) }} else null
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (passwordEnabled) {
                        // 关闭密码锁：验证当前密码
                        val saved = settingsPrefs.getString("app_password", "") ?: ""
                        if (passwordInput == saved) {
                            settingsPrefs.edit().remove("app_password").apply()
                            passwordEnabled = false
                            passwordInput = ""
                            passwordError = ""
                            showPasswordDialog = false
                        } else {
                            passwordError = "密码不正确"
                        }
                    } else {
                        // 设置密码锁
                        when {
                            passwordInput.length !in 4..6 || !passwordInput.all { it.isDigit() } -> {
                                passwordError = "请输入4-6位数字密码"
                            }
                            passwordInput != passwordConfirm -> {
                                passwordError = "两次输入的密码不一致"
                            }
                            else -> {
                                settingsPrefs.edit().putString("app_password", passwordInput).apply()
                                passwordEnabled = true
                                passwordInput = ""
                                passwordConfirm = ""
                                passwordError = ""
                                showPasswordDialog = false
                            }
                        }
                    }
                }) {
                    Text(
                        if (passwordEnabled) "确认关闭" else "确认设置",
                        color = if (passwordEnabled) AccentOrange else PrimaryPink,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    passwordInput = ""
                    passwordConfirm = ""
                    passwordError = ""
                }) {
                    Text("取消", color = TextHint)
                }
            }
        )
    }
}

@Composable
private fun DrawerSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PrimaryPink.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = PrimaryPink
            )
        }
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
                color = TextHint
            )
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = TextHint
        )
    }
}

@Composable
private fun PermissionRow(title: String, desc: String, granted: Boolean) {
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

// ══════════════════════════════════════════
// 流星效果
// ══════════════════════════════════════════
private data class ShootingStar(
    val startX: Float, val startY: Float,
    val angle: Float, // 角度（弧度）
    val speed: Float, // 速度
    val length: Float, // 尾巴长度
    val life: Float, // 总生命周期（秒）
    val delay: Float, // 延迟出现（秒）
    val thickness: Float // 粗细
)

@Composable
private fun ShootingStarsEffect(modifier: Modifier = Modifier) {
    val stars = remember {
        val rng = java.util.Random()
        List(6) {
            ShootingStar(
                startX = rng.nextFloat() * 0.5f + 0.05f, // 从5%-55%宽度出发
                startY = rng.nextFloat() * 0.25f + 0.05f, // 从顶部5%-30%出发
                angle = (Math.PI / 5 + rng.nextFloat() * Math.PI / 5).toFloat(), // 36°-72°倾斜
                speed = 0.5f + rng.nextFloat() * 0.4f,
                length = 50f + rng.nextFloat() * 80f,
                life = 0.8f + rng.nextFloat() * 0.6f,
                delay = rng.nextFloat() * 4f + it * 2f, // 更密集出现
                thickness = 1.5f + rng.nextFloat() * 2f // 更粗
            )
        }
    }

    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val startTime = System.nanoTime()
        while (true) {
            withFrameNanos { frameTime ->
                time = ((frameTime - startTime) / 1_000_000_000f) % 30f
            }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        stars.forEach { star ->
            val cycleTime = star.life + star.delay + 2f
            val t = (time % cycleTime) - star.delay
            if (t < 0f || t > star.life) return@forEach

            val progress = t / star.life
            val alphaRaw = when {
                progress < 0.2f -> progress / 0.2f
                progress > 0.7f -> (1f - progress) / 0.3f
                else -> 1f
            }
            val alpha = if (alphaRaw < 0f) 0f else if (alphaRaw > 1f) 1f else alphaRaw
            val finalAlpha = alpha * 0.8f

            val distance = progress * w * star.speed
            val cosA = kotlin.math.cos(star.angle.toDouble()).toFloat()
            val sinA = kotlin.math.sin(star.angle.toDouble()).toFloat()
            val currentX = star.startX * w + distance * cosA
            val currentY = star.startY * h + distance * sinA
            val tailX = currentX - star.length * cosA
            val tailY = currentY - star.length * sinA

            if (currentX < -50 || currentX > w + 50 || currentY < -50 || currentY > h + 50) return@forEach

            // 流星主体 - 金色渐变尾巴
            val starColor = Color(0xFFD4A574) // 暖金色
            drawLine(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        starColor.copy(alpha = finalAlpha),
                        Color(0xFFE8C9A0).copy(alpha = finalAlpha * 0.5f),
                        Color.Transparent
                    ),
                    start = Offset(currentX, currentY),
                    end = Offset(tailX, tailY)
                ),
                start = Offset(currentX, currentY),
                end = Offset(tailX, tailY),
                strokeWidth = star.thickness,
                cap = StrokeCap.Round
            )

            // 流星头部亮点
            drawCircle(
                color = starColor.copy(alpha = finalAlpha),
                radius = star.thickness * 1.5f,
                center = Offset(currentX, currentY)
            )
        }
    }
}

