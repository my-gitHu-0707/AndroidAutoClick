package com.autoclick.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.autoclick.app.utils.ClickSettings

class AutoClickService : AccessibilityService() {
    
    companion object {
        private const val TAG = "AutoClickService"
        var instance: AutoClickService? = null
            private set
    }
    
    private var isClicking = false
    private var clickHandler: Handler? = null
    private var clickRunnable: Runnable? = null
    private var clickCount = 0
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "AutoClickService connected")
        
        // 发送服务连接广播
        sendBroadcast(Intent("com.autoclick.app.SERVICE_CONNECTED"))
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        stopAutoClick()
        Log.d(TAG, "AutoClickService destroyed")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 这里可以处理无障碍事件，但对于连点器来说通常不需要
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "AutoClickService interrupted")
        stopAutoClick()
    }
    
    /**
     * 开始自动点击
     */
    fun startAutoClick() {
        if (isClicking) return
        
        isClicking = true
        clickCount = 0
        clickHandler = Handler(Looper.getMainLooper())
        
        clickRunnable = object : Runnable {
            override fun run() {
                if (isClicking) {
                    performClick()
                    clickHandler?.postDelayed(this, ClickSettings.clickInterval)
                }
            }
        }
        
        clickHandler?.post(clickRunnable!!)
        Log.d(TAG, "Auto click started")
        
        // 发送开始点击广播
        sendBroadcast(Intent("com.autoclick.app.CLICK_STARTED"))
    }
    
    /**
     * 停止自动点击
     */
    fun stopAutoClick() {
        isClicking = false
        clickRunnable?.let { clickHandler?.removeCallbacks(it) }
        clickHandler = null
        clickRunnable = null
        Log.d(TAG, "Auto click stopped")
        
        // 发送停止点击广播
        sendBroadcast(Intent("com.autoclick.app.CLICK_STOPPED"))
    }
    
    /**
     * 执行点击操作
     */
    private fun performClick() {
        val enabledPoints = ClickSettings.getEnabledClickPoints()

        if (enabledPoints.isEmpty()) {
            Log.w(TAG, "No enabled click points")
            return
        }

        // 为每个启用的点击点执行点击
        enabledPoints.forEach { point ->
            performSingleClick(point.x, point.y, point.id)
        }
    }

    /**
     * 执行单个点击
     */
    private fun performSingleClick(x: Float, y: Float, pointId: Int) {
        if (x <= 0 || y <= 0) {
            Log.w(TAG, "Invalid click coordinates for point $pointId: ($x, $y)")
            return
        }

        val path = Path()
        path.moveTo(x, y)

        val gestureBuilder = GestureDescription.Builder()
        val strokeDescription = GestureDescription.StrokeDescription(path, 0, 50)
        gestureBuilder.addStroke(strokeDescription)

        val gesture = gestureBuilder.build()

        val result = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                clickCount++
                Log.d(TAG, "Click performed at ($x, $y) for point $pointId, total count: $clickCount")

                // 发送点击计数广播
                val intent = Intent("com.autoclick.app.CLICK_COUNT_UPDATED")
                intent.putExtra("count", clickCount)
                intent.putExtra("point_id", pointId)
                sendBroadcast(intent)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w(TAG, "Click gesture cancelled for point $pointId")
            }
        }, null)

        if (!result) {
            Log.e(TAG, "Failed to dispatch click gesture for point $pointId")
        }
    }
    
    /**
     * 获取当前点击状态
     */
    fun isAutoClicking(): Boolean = isClicking
    
    /**
     * 获取点击次数
     */
    fun getClickCount(): Int = clickCount
}
