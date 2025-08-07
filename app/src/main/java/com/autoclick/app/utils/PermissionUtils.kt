package com.autoclick.app.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import android.util.Log

object PermissionUtils {
    
    /**
     * 检查无障碍服务是否已启用
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == context.packageName) {
                return true
            }
        }
        return false
    }
    
    /**
     * 打开无障碍服务设置页面
     */
    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            Toast.makeText(context, "请在设置中找到并启用 Android Auto Click 的无障碍服务", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("PermissionUtils", "Failed to open accessibility settings", e)
            Toast.makeText(context, "无法打开无障碍设置，请手动前往 设置 > 无障碍 中启用服务", Toast.LENGTH_LONG).show()

            // 备用方案：打开应用设置页面
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${context.packageName}")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e("PermissionUtils", "Failed to open app settings", e2)
            }
        }
    }
    
    /**
     * 检查悬浮窗权限是否已授予
     */
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    /**
     * 打开悬浮窗权限设置页面
     */
    fun openOverlaySettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = Uri.parse("package:${context.packageName}")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                Toast.makeText(context, "请允许应用显示在其他应用上层", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "当前系统版本无需悬浮窗权限", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("PermissionUtils", "Failed to open overlay settings", e)
            Toast.makeText(context, "无法打开悬浮窗设置，请手动前往 设置 > 应用权限 > 悬浮窗 中授权", Toast.LENGTH_LONG).show()

            // 备用方案：打开应用设置页面
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${context.packageName}")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e("PermissionUtils", "Failed to open app settings", e2)
            }
        }
    }
    
    /**
     * 检查所有必要权限是否已授予
     */
    fun hasAllPermissions(context: Context): Boolean {
        return isAccessibilityServiceEnabled(context) && canDrawOverlays(context)
    }
}
