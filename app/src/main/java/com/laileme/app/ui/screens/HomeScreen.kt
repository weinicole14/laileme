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
            // 缩小的圆环，偏移对准月亮内圈（月牙左厚右薄，内圈偏右偏上）
            Box(
                modifier = Modifier
                    .size(115.dp)
                    .offset(x = 18.dp, y = (-41).dp),
                contentAlignment = Alignment.Center
            ) {
                val ringGender = AuthManager.userState.collectAsState().value?.gender ?: "female"
                CycleRing(uiState)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (ringGender == "male") {
                        // 男性用户 - 显示伴侣模式
                        if (hasRecord) {
                            Text("伴侣", fontSize = 8.sp, color = TextHint)
                            if (uiState.isInPeriod) {
                                Text(
                                    "${uiState.daysUntilPeriodEnd}",
                                    fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PeriodRed
                                )
                                Text("天后结束", fontSize = 10.sp, color = TextSecondary)
                            } else {
                                Text(
                                    "${uiState.daysUntilNextPeriod}",
                                    fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4FC3F7)
                                )
                                Text("天后来", fontSize = 10.sp, color = TextSecondary)
                            }
                            Text("周期第 ${uiState.cycleDay} 天", fontSize = 8.sp, color = TextHint)
                        } else {
                            Text("♡", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4FC3F7))
                            Text("伴侣模式", fontSize = 10.sp, color = TextSecondary)
                        }
                    } else {
                    // 女性用户 - 原有逻辑
                    if (hasRecord) {
                if (uiState.isFirstRecord && uiState.isInPeriod) {
                    // 首次记录中（不足2条完整记录），无法预测结束时间
                    Text("经期", fontSize = 8.sp, color = TextHint)
                    Text("记录中", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PeriodRed)
                    Text("第${uiState.cycleDay}天", fontSize = 10.sp, color = TextSecondary)
                } else if (uiState.isFirstRecord && !uiState.isInPeriod) {
                    // 首次记录已结束但还没有第二次记录，无法推算周期
                    Text("周期", fontSize = 8.sp, color = TextHint)
                    Text("收集中", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryPink)
                    Text("等待下次记录", fontSize = 10.sp, color = TextSecondary)
                } else if (uiState.isInPeriod) {
                    Text("预计", fontSize = 8.sp, color = TextHint)
                    Text(
                        "${uiState.daysUntilPeriodEnd}",
                        fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PeriodRed
                    )
                    Text("天后结束", fontSize = 10.sp, color = TextSecondary)
                    Text("周期第 ${uiState.cycleDay} 天", fontSize = 8.sp, color = TextHint)
                } else {
                    Text("预计", fontSize = 8.sp, color = TextHint)
                    Text(
                        "${uiState.daysUntilNextPeriod}",
                        fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PrimaryPink
                    )
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

        Spacer(modifier = Modifier.height(24.dp))

        // 只统计已完成（有结束日期）的记录
        val completedRecords = uiState.records.filter { it.endDate != null }
        val latestCompleted = completedRecords.firstOrNull()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatusCard("周期", if (latestCompleted != null && !uiState.isFirstRecord) "${latestCompleted.cycleLength}" else "--", "天", AccentTeal)
            StatusCard("经期", if (latestCompleted != null) "${latestCompleted.periodLength}" else "--", "天", PrimaryPink)
            StatusCard("记录", if (completedRecords.isNotEmpty()) "${completedRecords.size}" else "--", "次", AccentOrange)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 小贴士（随机轮播）
        var tipIndex by remember { mutableIntStateOf((Math.random() * periodTips.size).toInt()) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(6000)
                tipIndex = (tipIndex + (1..periodTips.size - 1).random()) % periodTips.size
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = LightPink),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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

        Spacer(modifier = Modifier.height(12.dp))

        // 操作按钮 - 仅女性用户显示
        val userGender = AuthManager.userState.collectAsState().value?.gender ?: "female"
        if (userGender == "female") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
        } else if (userGender == "male") {
            // ── 男性用户：关怀按钮（每天一次）──
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
private fun StatusCard(title: String, value: String, unit: String, color: Color) {
    Card(
        modifier = Modifier.width(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 10.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
                Text(unit, fontSize = 9.sp, color = TextHint, modifier = Modifier.padding(bottom = 1.dp))
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
    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
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
                Text(
                    if (notificationEnabled) "当前经期提醒已开启，是否关闭？\n关闭后将不再收到经期预测通知。"
                    else "当前经期提醒已关闭，是否开启？\n开启后将在经期临近时提醒您。",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newState = !notificationEnabled
                    settingsPrefs.edit().putBoolean("period_notification", newState).apply()
                    notificationEnabled = newState
                    showNotificationDialog = false
                }) {
                    Text(
                        if (notificationEnabled) "关闭提醒" else "开启提醒",
                        color = PrimaryPink,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("取消", color = TextHint)
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

