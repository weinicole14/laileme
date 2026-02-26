package com.laileme.app.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * 主题配色定义
 */
data class ThemeColors(
    val key: String,          // 存储用的key
    val name: String,         // 显示名
    val primary: Color,       // 主色
    val lightPrimary: Color,  // 浅色背景
    val previewColor: Color = primary  // 预览色块
)

object ThemeManager {

    // 所有可选主题
    val themes = listOf(
        ThemeColors("pink", "樱花粉", Color(0xFFFF8A9B), Color(0xFFFFE4E8)),
        ThemeColors("rose", "玫瑰红", Color(0xFFE8546D), Color(0xFFFDE4EA)),
        ThemeColors("peach", "蜜桃橙", Color(0xFFFF9966), Color(0xFFFFF0E5)),
        ThemeColors("lemon", "柠檬黄", Color(0xFFE8B445), Color(0xFFFFF8E1)),
        ThemeColors("mint", "薄荷绿", Color(0xFF5ECFB1), Color(0xFFE0F7F0)),
        ThemeColors("sky", "天空蓝", Color(0xFF5B9BD5), Color(0xFFE3F0FB)),
        ThemeColors("haze", "雾霾蓝", Color(0xFF7BA7BC), Color(0xFFE8F1F5)),
        ThemeColors("lavender", "薰衣草", Color(0xFF9B8EC4), Color(0xFFF0EBF8)),
        ThemeColors("lilac", "丁香紫", Color(0xFFB39DDB), Color(0xFFF3EEFB)),
        ThemeColors("cocoa", "可可棕", Color(0xFFA8896C), Color(0xFFF5F0EB)),
        ThemeColors("teal", "青瓷绿", Color(0xFF4DB6AC), Color(0xFFE0F2F1)),
        ThemeColors("coral", "珊瑚色", Color(0xFFFF7F7F), Color(0xFFFFEBEB)),
    )

    // 当前主色调（响应式）
    var currentPrimary by mutableStateOf(Color(0xFFFF8A9B))
        private set
    var currentLightPrimary by mutableStateOf(Color(0xFFFFE4E8))
        private set

    // 当前主题key
    var currentThemeKey by mutableStateOf("pink")
        private set

    /** 初始化：从SP读取已保存的主题 */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences("laileme_theme", Context.MODE_PRIVATE)
        val savedKey = prefs.getString("theme_key", "pink") ?: "pink"
        applyThemeByKey(savedKey)
    }

    /** 切换主题并持久化 */
    fun setTheme(context: Context, themeKey: String) {
        applyThemeByKey(themeKey)
        context.getSharedPreferences("laileme_theme", Context.MODE_PRIVATE)
            .edit()
            .putString("theme_key", themeKey)
            .apply()
    }

    private fun applyThemeByKey(key: String) {
        val theme = themes.find { it.key == key } ?: themes[0]
        currentThemeKey = theme.key
        currentPrimary = theme.primary
        currentLightPrimary = theme.lightPrimary
    }
}
