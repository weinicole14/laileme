package com.laileme.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * 各品牌手机自启动/后台管理/电池优化 一键跳转工具
 * 支持：华为/荣耀/小米/红米/OPPO/realme/vivo/iQOO/一加/三星 等
 * 策略：按优先级依次尝试多个 Intent，哪个能打开就用哪个
 */
object AutoStartHelper {
    private const val TAG = "AutoStartHelper"

    /**
     * 跳转到自启动管理页面
     * 一键尝试所有品牌的自启动设置页，找到能打开的就跳转
     */
    fun jumpToAutoStart(context: Context): Boolean {
        val intents = listOf(
            // 华为/荣耀 - 启动管理
            intent("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            intent("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
            intent("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
            // 小米/红米 - 自启动管理
            intent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            // OPPO/realme - 自启动管理
            intent("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            intent("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            intent("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            // vivo/iQOO - 后台高耗电
            intent("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            intent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            intent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            // 一加
            intent("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
            // 三星
            intent("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
            // 联想/ZUI
            intent("com.lenovo.security", "com.lenovo.security.purebackground.PureBackgroundActivity"),
            // 魅族
            intent("com.meizu.safe", "com.meizu.safe.security.SHOW_APPSEC"),
        )
        return tryLaunch(context, intents)
    }

    /**
     * 跳转到后台运行/省电策略页面
     * 让用户把应用设为"不受限制"或加入后台白名单
     */
    fun jumpToBackgroundManager(context: Context): Boolean {
        val intents = listOf(
            // 华为/荣耀 - 电池优化/应用启动管理
            intent("com.huawei.systemmanager", "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"),
            intent("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
            // 小米/红米 - 省电策略
            intent("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"),
            Intent().apply { setClassName("com.miui.securitycenter", "com.miui.securitycenter.MainActivity") },
            // OPPO/realme
            intent("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"),
            intent("com.oplus.battery", "com.oplus.powermanager.fuelgaue.PowerUsageModelActivity"),
            // vivo/iQOO
            intent("com.vivo.abe", "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"),
            intent("com.iqoo.powermanager", "com.iqoo.powermanager.PowerManagerActivity"),
            // 一加
            intent("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
            // 三星
            intent("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.usage.BatteryActivity"),
        )

        // 先尝试品牌页面，失败则跳转系统电池优化
        if (tryLaunch(context, intents)) return true

        // 通用回退：系统电池优化设置
        return try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 跳转到应用详情设置页（通用兜底）
     */
    fun jumpToAppDetail(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 获取当前手机品牌名称
     */
    fun getBrandName(): String {
        val brand = Build.BRAND?.lowercase() ?: ""
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        return when {
            brand.contains("huawei") || manufacturer.contains("huawei") -> "华为"
            brand.contains("honor") || manufacturer.contains("honor") -> "荣耀"
            brand.contains("xiaomi") || manufacturer.contains("xiaomi") -> "小米"
            brand.contains("redmi") -> "红米"
            brand.contains("oppo") || manufacturer.contains("oppo") -> "OPPO"
            brand.contains("realme") || manufacturer.contains("realme") -> "realme"
            brand.contains("vivo") || manufacturer.contains("vivo") -> "vivo"
            brand.contains("iqoo") -> "iQOO"
            brand.contains("oneplus") || manufacturer.contains("oneplus") -> "一加"
            brand.contains("samsung") || manufacturer.contains("samsung") -> "三星"
            brand.contains("meizu") || manufacturer.contains("meizu") -> "魅族"
            brand.contains("lenovo") || manufacturer.contains("lenovo") -> "联想"
            else -> Build.BRAND ?: "未知"
        }
    }

    // ── 内部工具 ──

    private fun intent(pkg: String, cls: String): Intent {
        return Intent().apply {
            component = ComponentName(pkg, cls)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun tryLaunch(context: Context, intents: List<Intent>): Boolean {
        for (intent in intents) {
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Log.i(TAG, "成功跳转: ${intent.component}")
                    return true
                }
            } catch (_: Exception) {
                continue
            }
        }
        return false
    }
}
