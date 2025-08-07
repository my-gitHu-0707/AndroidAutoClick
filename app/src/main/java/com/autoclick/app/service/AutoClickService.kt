package com.autoclick.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.autoclick.app.utils.ClickSettings
import com.autoclick.app.service.ClickPointService
import java.util.concurrent.atomic.AtomicBoolean

class AutoClickService : AccessibilityService() {
    
    companion object {
        private const val TAG = "AutoClickService"
        var instance: AutoClickService? = null
            private set
    }
    
    private val isClicking = AtomicBoolean(false)
    private var clickHandlerThread: HandlerThread? = null
    private var clickHandler: Handler? = null
    private var clickRunnable: Runnable? = null
    private var clickCount = 0
    private var currentPointIndex = 0
    private var mainHandler: Handler = Handler(Looper.getMainLooper())
    
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
        cleanupHandlerThread()
        Log.d(TAG, "AutoClickService destroyed")
    }

    private fun cleanupHandlerThread() {
        clickHandlerThread?.quitSafely()
        clickHandlerThread = null
        clickHandler = null
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
        if (isClicking.get()) return

        isClicking.set(true)
        clickCount = 0
        currentPointIndex = 0

        // 创建后台线程处理点击操作
        clickHandlerThread = HandlerThread("AutoClickThread").apply {
            start()
        }
        clickHandler = Handler(clickHandlerThread!!.looper)

        clickRunnable = object : Runnable {
            override fun run() {
                // 高优先级检查停止标志，确保立即响应
                if (!isClicking.get()) {
                    Log.d(TAG, "Click loop stopped by flag check")
                    return
                }

                performNextClick()

                // 再次检查停止标志，确保在延迟前能立即停止
                if (!isClicking.get()) {
                    Log.d(TAG, "Click loop stopped after click execution")
                    return
                }

                // 使用当前点击位置的间隔
                val interval = getCurrentClickInterval()
                clickHandler?.postDelayed(this, interval)
            }
        }

        clickHandler?.post(clickRunnable!!)
        Log.d(TAG, "Auto click started with interval: ${ClickSettings.clickInterval}ms")

        // 在主线程发送广播
        mainHandler.post {
            sendBroadcast(Intent("com.autoclick.app.CLICK_STARTED"))
        }
    }
    
    /**
     * 停止自动点击 - 高优先级，立即响应
     */
    fun stopAutoClick() {
        Log.d(TAG, "Stop auto click requested - immediate response")

        // 立即设置停止标志，确保最高优先级
        isClicking.set(false)

        // 立即移除所有待执行的点击任务
        clickRunnable?.let {
            clickHandler?.removeCallbacks(it)
            Log.d(TAG, "Removed pending click callbacks")
        }

        // 立即清理线程资源
        cleanupHandlerThread()
        clickRunnable = null

        Log.d(TAG, "Auto click stopped immediately")

        // 在主线程立即发送广播
        mainHandler.post {
            sendBroadcast(Intent("com.autoclick.app.CLICK_STOPPED"))
        }
    }
    
    /**
     * 执行下一个点击操作
     */
    private fun performNextClick() {
        val clickPointService = ClickPointService.instance
        val clickPoints = clickPointService?.getClickPoints() ?: emptyList()

        if (clickPoints.isEmpty()) {
            // 如果没有悬浮点击位置，使用默认位置
            performClick(ClickSettings.clickX, ClickSettings.clickY)
            return
        }

        // 循环点击各个位置
        val currentPoint = clickPoints[currentPointIndex % clickPoints.size]
        performClick(currentPoint.getX(), currentPoint.getY())

        // 移动到下一个点击位置
        currentPointIndex = (currentPointIndex + 1) % clickPoints.size
    }

    /**
     * 获取当前点击位置的间隔
     */
    private fun getCurrentClickInterval(): Long {
        val clickPointService = ClickPointService.instance
        val clickPoints = clickPointService?.getClickPoints() ?: emptyList()

        if (clickPoints.isEmpty()) {
            return ClickSettings.clickInterval
        }

        val currentPoint = clickPoints[currentPointIndex % clickPoints.size]
        return currentPoint.clickInterval
    }

    /**
     * 执行点击操作
     */
    private fun performClick(x: Float, y: Float) {
        if (x <= 0 || y <= 0) {
            Log.w(TAG, "Invalid click coordinates: ($x, $y)")
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
                Log.d(TAG, "Click performed at ($x, $y), count: $clickCount")

                // 在主线程发送点击计数广播
                mainHandler.post {
                    val intent = Intent("com.autoclick.app.CLICK_COUNT_UPDATED")
                    intent.putExtra("count", clickCount)
                    sendBroadcast(intent)
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w(TAG, "Click gesture cancelled")
            }
        }, null)
        
        if (!result) {
            Log.e(TAG, "Failed to dispatch click gesture")
        }
    }
    
    /**
     * 获取当前点击状态
     */
    fun isAutoClicking(): Boolean = isClicking.get()
    
    /**
     * 获取点击次数
     */
    fun getClickCount(): Int = clickCount
}
