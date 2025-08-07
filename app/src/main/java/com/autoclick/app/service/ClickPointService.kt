package com.autoclick.app.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.autoclick.app.R
import com.autoclick.app.utils.ClickSettings

class ClickPointService : Service() {
    
    companion object {
        private const val TAG = "ClickPointService"
        var instance: ClickPointService? = null
            private set
    }
    
    private var windowManager: WindowManager? = null
    private val clickPointViews = mutableListOf<ClickPointView>()
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Log.d(TAG, "ClickPointService created")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ADD_CLICK_POINT" -> {
                val x = intent.getFloatExtra("x", 500f)
                val y = intent.getFloatExtra("y", 500f)
                addClickPoint(x, y)
            }
            "REMOVE_ALL_POINTS" -> {
                removeAllClickPoints()
            }
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        removeAllClickPoints()
        instance = null
        Log.d(TAG, "ClickPointService destroyed")
    }
    
    private fun addClickPoint(x: Float, y: Float) {
        try {
            val clickPointView = ClickPointView(x, y)
            clickPointViews.add(clickPointView)
            windowManager?.addView(clickPointView.view, clickPointView.layoutParams)
            Log.d(TAG, "Added click point at ($x, $y)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add click point", e)
        }
    }
    
    private fun removeAllClickPoints() {
        clickPointViews.forEach { clickPoint ->
            try {
                windowManager?.removeView(clickPoint.view)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove click point", e)
            }
        }
        clickPointViews.clear()
    }
    
    inner class ClickPointView(initialX: Float, initialY: Float) {
        val view: ImageView
        val layoutParams: WindowManager.LayoutParams
        
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var initialViewX = 0
        private var initialViewY = 0
        private var isDragging = false
        private var longPressRunnable: Runnable? = null
        
        // 点击配置
        var clickInterval = 1000L
        var clickCount = -1 // -1表示无限次
        
        init {
            // 创建圆形ImageView
            view = ImageView(this@ClickPointService).apply {
                setImageResource(R.drawable.ic_click_point)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            
            // 设置悬浮窗参数
            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            
            layoutParams = WindowManager.LayoutParams(
                120, // width
                120, // height
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = initialX.toInt()
                y = initialY.toInt()
            }
            
            setupTouchListener()
        }
        
        private fun setupTouchListener() {
            view.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        initialViewX = layoutParams.x
                        initialViewY = layoutParams.y
                        isDragging = false
                        
                        // 设置长按检测
                        longPressRunnable = Runnable {
                            if (!isDragging) {
                                onLongPress()
                            }
                        }
                        view.postDelayed(longPressRunnable, 500) // 500ms长按
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        
                        // 如果移动距离超过阈值，认为是拖拽
                        if (!isDragging && (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10)) {
                            isDragging = true
                            longPressRunnable?.let { view.removeCallbacks(it) }
                        }
                        
                        if (isDragging) {
                            layoutParams.x = (initialViewX + deltaX).toInt()
                            layoutParams.y = (initialViewY + deltaY).toInt()
                            windowManager?.updateViewLayout(view, layoutParams)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        longPressRunnable?.let { view.removeCallbacks(it) }
                        
                        if (isDragging) {
                            // 拖拽结束，更新点击位置
                            updateClickPosition()
                        }
                        true
                    }
                    else -> false
                }
            }
        }
        
        private fun onLongPress() {
            // TODO: 显示配置对话框
            Log.d(TAG, "Long press detected on click point")
        }
        
        private fun updateClickPosition() {
            // 更新ClickSettings中的位置
            ClickSettings.clickX = (layoutParams.x + 60).toFloat() // 圆圈中心
            ClickSettings.clickY = (layoutParams.y + 60).toFloat()
            Log.d(TAG, "Updated click position to (${ClickSettings.clickX}, ${ClickSettings.clickY})")
        }
        
        fun getX(): Float = (layoutParams.x + 60).toFloat()
        fun getY(): Float = (layoutParams.y + 60).toFloat()
    }
}
