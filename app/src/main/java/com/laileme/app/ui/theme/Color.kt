package com.laileme.app.ui.theme

import androidx.compose.ui.graphics.Color

// 柔和配色方案 — 主色调跟随主题动态变化
val PrimaryPink: Color get() = ThemeManager.currentPrimary       // 主色（动态）
val LightPink: Color get() = ThemeManager.currentLightPrimary    // 浅色背景（动态）
val AccentTeal = Color(0xFF5ECFB1)       // 薄荷绿强调色
val AccentOrange = Color(0xFFFFB347)     // 橙色
val AccentBlue = Color(0xFF87CEEB)       // 天蓝色

val PeriodRed = Color(0xFFFF6B6B)        // 经期红色
val PredictPeriod = Color(0xFFFFB3B3)    // 预测经期浅红
val OvulationOrange = Color(0xFFFFB347)  // 排卵期橙色
val OvulationHeart = Color(0xFFFF69B4)   // 记录排卵心形
val PredictOvulation = Color(0xFFFFB6C1) // 预测排卵
val FertileGreen = Color(0xFF81C784)     // 易孕期绿色

val CardBackground = Color(0xFFF8FBFF)   // 卡片背景
val Background = Color(0xFFF5F8FA)       // 页面背景
val BottomSheetBg = Color(0xFFE8F4F8)    // 底部面板背景

val TextPrimary = Color(0xFF2D3436)      // 主文字
val TextSecondary = Color(0xFF636E72)    // 次要文字
val TextHint = Color(0xFFB2BEC3)         // 提示文字

val TodayGreen = Color(0xFF5ECFB1)       // 今天标记
val WeekendText: Color get() = ThemeManager.currentPrimary  // 周末文字颜色（跟随主题）

val NavSelected: Color get() = ThemeManager.currentPrimary  // 导航选中（跟随主题）
val NavUnselected = Color(0xFFB2BEC3)    // 导航未选中
