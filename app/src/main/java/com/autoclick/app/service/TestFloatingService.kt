package com.autoclick.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.autoclick.app.R
import com.autoclick.app.utils.PermissionUtils

/**
 * 测试悬浮窗服务 - 用于验证悬浮窗权限和基本功能
 */
class TestFloatingService : Service() {
    
    companion object {
        private const val TAG = "TestFloatingService"
    }
    
    private var windowManager: WindowManager? = null
    private var testView: TextView? = null
    private var isViewAdded = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TestFloatingService created")
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createTestFloatingView()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "TestFloatingService destroyed")
        removeTestView()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createTestFloatingView() {
        try {
            Log.d(TAG, "Creating test floating view...")
            
            // 检查权限
            if (!PermissionUtils.canDrawOverlays(this)) {
                Log.e(TAG, "No overlay permission")
                Toast.makeText(this, "没有悬浮窗权限", Toast.LENGTH_LONG).show()
                stopSelf()
                return
            }
            
            // 创建简单的测试视图
            testView = TextView(this).apply {
                text = "测试悬浮窗\n权限正常"
                textSize = 16f
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#80000000"))
                setPadding(20, 20, 20, 20)
            }
            
            // 设置窗口参数
            val layoutParams = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.CENTER
            }
            
            // 添加到窗口管理器
            windowManager?.addView(testView, layoutParams)
            isViewAdded = true
            
            Log.d(TAG, "Test floating view created successfully")
            Toast.makeText(this, "测试悬浮窗显示成功", Toast.LENGTH_SHORT).show()
            
            // 3秒后自动关闭
            testView?.postDelayed({
                stopSelf()
            }, 3000)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create test floating view", e)
            Toast.makeText(this, "测试悬浮窗创建失败: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }
    
    private fun removeTestView() {
        try {
            if (isViewAdded && testView != null) {
                windowManager?.removeView(testView)
                isViewAdded = false
                testView = null
                Log.d(TAG, "Test floating view removed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove test floating view", e)
        }
    }
}
