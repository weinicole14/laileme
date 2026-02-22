package com.laileme.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import com.laileme.app.ui.PeriodUiState
import com.laileme.app.ui.theme.*
import java.util.Calendar

// 页面状态枚举
private enum class ProfilePage { MAIN, SETTINGS, PROFILE_INFO }

@Composable
fun ProfileScreen(
    uiState: PeriodUiState,
    onSaveSettings: (Int, Int) -> Unit,
    onSaveMode: (String) -> Unit,
    onSubPageChanged: (Boolean) -> Unit = {}
) {
    var currentPage by remember { mutableStateOf(ProfilePage.MAIN) }
    var previousPage by remember { mutableStateOf(ProfilePage.MAIN) }

    // 通知外部当前是否在子页面
    LaunchedEffect(currentPage) {
        onSubPageChanged(currentPage != ProfilePage.MAIN)
    }

    // 二级页面拦截系统返回键，返回主页面
    BackHandler(enabled = currentPage != ProfilePage.MAIN) {
        previousPage = currentPage
        currentPage = ProfilePage.MAIN
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                val goingForward = targetState.ordinal > previousPage.ordinal
                if (goingForward) {
                    (slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = tween(300)
                    ) + fadeOut(tween(150)))
                } else {
                    (slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(
                        targetOffsetX = { it / 3 },
                        animationSpec = tween(300)
                    ) + fadeOut(tween(150)))
                }.using(SizeTransform(clip = false))
            },
            label = "profile_pages"
        ) { page ->
            when (page) {
                ProfilePage.MAIN -> ProfileContent(
                    onOpenSettings = {
                        previousPage = ProfilePage.MAIN
                        currentPage = ProfilePage.SETTINGS
                    },
                    onOpenProfile = {
                        previousPage = ProfilePage.MAIN
                        currentPage = ProfilePage.PROFILE_INFO
                    }
                )
                ProfilePage.SETTINGS -> SettingsContent(
                    uiState = uiState,
                    onSaveSettings = onSaveSettings,
                    onSaveMode = onSaveMode,
                    onBack = {
                        previousPage = ProfilePage.SETTINGS
                        currentPage = ProfilePage.MAIN
                    }
                )
                ProfilePage.PROFILE_INFO -> ProfileInfoContent(
                    onBack = {
                        previousPage = ProfilePage.PROFILE_INFO
                        currentPage = ProfilePage.MAIN
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("laileme_profile", Context.MODE_PRIVATE) }
    val nickname = prefs.getString("nickname", "") ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(12.dp)
            .padding(bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // 头像
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(PrimaryPink.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = PrimaryPink
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (nickname.isNotEmpty()) nickname else "我的来了么",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "记录每一天的美好",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 个人档案入口
        ProfileMenuCard(
            icon = Icons.Outlined.Person,
            title = "个人档案",
            subtitle = "生日、身高、体重、血型",
            onClick = onOpenProfile
        )

        // 设置入口
        ProfileMenuCard(
            icon = Icons.Outlined.Settings,
            title = "设置",
            subtitle = "经期设置、通知、外观",
            onClick = onOpenSettings
        )
    }
}

@Composable
private fun ProfileMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryPink.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = PrimaryPink
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextHint
                )
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

// ──────────────── 个人档案页面 ────────────────

@Composable
private fun ProfileInfoContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("laileme_profile", Context.MODE_PRIVATE) }

    var nickname by remember { mutableStateOf(prefs.getString("nickname", "") ?: "") }
    var birthYear by remember { mutableStateOf(prefs.getString("birth_year", "") ?: "") }
    var birthMonth by remember { mutableStateOf(prefs.getString("birth_month", "") ?: "") }
    var birthDay by remember { mutableStateOf(prefs.getString("birth_day", "") ?: "") }
    var height by remember { mutableStateOf(prefs.getString("height", "") ?: "") }
    var weight by remember { mutableStateOf(prefs.getString("weight", "") ?: "") }
    var bloodType by remember { mutableStateOf(prefs.getString("blood_type", "") ?: "") }
    var saved by remember { mutableStateOf(false) }

    // 计算年龄
    val age = remember(birthYear, birthMonth, birthDay) {
        try {
            val y = birthYear.toIntOrNull() ?: return@remember null
            val m = birthMonth.toIntOrNull() ?: return@remember null
            val d = birthDay.toIntOrNull() ?: return@remember null
            val now = Calendar.getInstance()
            var a = now.get(Calendar.YEAR) - y
            if (now.get(Calendar.MONTH) + 1 < m || (now.get(Calendar.MONTH) + 1 == m && now.get(Calendar.DAY_OF_MONTH) < d)) {
                a--
            }
            if (a >= 0) a else null
        } catch (_: Exception) { null }
    }

    // 计算BMI
    val bmi = remember(height, weight) {
        try {
            val h = height.toFloatOrNull() ?: return@remember null
            val w = weight.toFloatOrNull() ?: return@remember null
            if (h > 0 && w > 0) {
                val hm = h / 100f
                w / (hm * hm)
            } else null
        } catch (_: Exception) { null }
    }

    val bmiCategory = remember(bmi) {
        when {
            bmi == null -> null
            bmi < 18.5f -> "偏瘦"
            bmi < 24f -> "正常"
            bmi < 28f -> "偏胖"
            else -> "肥胖"
        }
    }

    val bmiColor = remember(bmiCategory) {
        when (bmiCategory) {
            "偏瘦" -> AccentBlue
            "正常" -> AccentTeal
            "偏胖" -> AccentOrange
            "肥胖" -> PeriodRed
            else -> TextHint
        }
    }

    val bmiAdvice = remember(bmiCategory) {
        when (bmiCategory) {
            "偏瘦" -> "建议适当增加营养摄入，保证充足蛋白质和碳水化合物，有助于维持健康体重和正常月经周期~"
            "正常" -> "体重很健康哦！继续保持均衡饮食和适度运动，身体棒棒的~"
            "偏胖" -> "建议适当控制饮食，增加有氧运动。体重过重可能影响月经规律，保持健康体重很重要哦~"
            "肥胖" -> "建议咨询医生制定科学的减重计划。肥胖可能导致月经不调等问题，健康减重对经期调理很有帮助~"
            else -> null
        }
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
                text = "个人档案",
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
            // ── 基本信息 ──
            ProfileSectionTitle("基本信息")

            ProfileTextField(
                label = "昵称",
                value = nickname,
                onValueChange = { nickname = it; saved = false },
                placeholder = "输入你的昵称"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 生日（年月日）
            Text("生日", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ProfileTextField(
                    label = "",
                    value = birthYear,
                    onValueChange = { birthYear = it.filter { c -> c.isDigit() }.take(4); saved = false },
                    placeholder = "年",
                    modifier = Modifier.weight(1.2f),
                    isNumber = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                ProfileTextField(
                    label = "",
                    value = birthMonth,
                    onValueChange = { birthMonth = it.filter { c -> c.isDigit() }.take(2); saved = false },
                    placeholder = "月",
                    modifier = Modifier.weight(1f),
                    isNumber = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                ProfileTextField(
                    label = "",
                    value = birthDay,
                    onValueChange = { birthDay = it.filter { c -> c.isDigit() }.take(2); saved = false },
                    placeholder = "日",
                    modifier = Modifier.weight(1f),
                    isNumber = true
                )
            }

            if (age != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentTeal.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Text(
                        "🎂 ${age}岁",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 身体数据 ──
            ProfileSectionTitle("身体数据")

            Row(modifier = Modifier.fillMaxWidth()) {
                ProfileTextField(
                    label = "身高 (cm)",
                    value = height,
                    onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' }.take(5); saved = false },
                    placeholder = "160",
                    modifier = Modifier.weight(1f),
                    isNumber = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                ProfileTextField(
                    label = "体重 (kg)",
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' }.take(5); saved = false },
                    placeholder = "50",
                    modifier = Modifier.weight(1f),
                    isNumber = true
                )
            }

            // BMI 显示
            if (bmi != null && bmiCategory != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bmiColor.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "BMI",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = bmiColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "%.1f".format(bmi),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = bmiColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bmiColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    bmiCategory,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bmiColor
                                )
                            }
                        }
                        if (bmiAdvice != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                bmiAdvice,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 血型 ──
            ProfileSectionTitle("血型")

            val bloodTypes = listOf("A", "B", "AB", "O", "未知")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bloodTypes.forEach { type ->
                    val isSelected = bloodType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) PrimaryPink.copy(alpha = 0.15f) else Color.White)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) PrimaryPink else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { bloodType = type; saved = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            type,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PrimaryPink else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 保存按钮
            Button(
                onClick = {
                    prefs.edit()
                        .putString("nickname", nickname)
                        .putString("birth_year", birthYear)
                        .putString("birth_month", birthMonth)
                        .putString("birth_day", birthDay)
                        .putString("height", height)
                        .putString("weight", weight)
                        .putString("blood_type", bloodType)
                        .apply()
                    saved = true
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
            ) {
                Icon(
                    imageVector = if (saved) Icons.Outlined.CheckCircle else Icons.Outlined.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (saved) "已保存 ✓" else "保存档案",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = PrimaryPink,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 4.dp)
    )
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isNumber: Boolean = false
) {
    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(
                label, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = TextPrimary, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 13.sp, color = TextHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPink,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        )
    }
}
