package com.autoclick.app.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.autoclick.app.R
import com.autoclick.app.ClickPointConfigActivity
import com.autoclick.app.utils.ClickSettings

class ClickPointService : Service() {
    
    companion object {
        private const val TAG = "ClickPointService"
        var instance: ClickPointService? = null
            private set
    }
    
    private var windowManager: WindowManager? = null
    private val clickPointViews = mutableListOf<ClickPointView>()

    private val configReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.autoclick.app.CLICK_POINT_CONFIG_UPDATED") {
                val interval = intent.getLongExtra(ClickPointConfigActivity.RESULT_INTERVAL, 1000L)
                val count = intent.getIntExtra(ClickPointConfigActivity.RESULT_COUNT, -1)

                // 更新最后一个点击点的配置（简化处理）
                if (clickPointViews.isNotEmpty()) {
                    val lastPoint = clickPointViews.last()
                    lastPoint.clickInterval = interval
                    lastPoint.clickCount = count
                    Log.d(TAG, "Updated click point config: interval=$interval, count=$count")
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // 注册广播接收器
        val filter = IntentFilter("com.autoclick.app.CLICK_POINT_CONFIG_UPDATED")
        registerReceiver(configReceiver, filter)

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

        // 注销广播接收器
        try {
            unregisterReceiver(configReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister receiver", e)
        }

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
        val view: View
        val layoutParams: WindowManager.LayoutParams
        private val pointNumber = clickPointViews.size + 1
        
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
            // 创建十字线视图
            view = LayoutInflater.from(this@ClickPointService)
                .inflate(R.layout.click_position_crosshair, null)

            // 设置编号
            val tvNumber = view.findViewById<TextView>(R.id.tvNumber)
            tvNumber.text = pointNumber.toString()
            
            // 设置悬浮窗参数
            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            
            layoutParams = WindowManager.LayoutParams(
                50, // width - 小圆圈尺寸
                50, // height - 小圆圈尺寸
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
            Log.d(TAG, "Long press detected on click point")

            // 启动配置Activity
            val intent = Intent(this@ClickPointService, ClickPointConfigActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(ClickPointConfigActivity.EXTRA_INTERVAL, clickInterval)
                putExtra(ClickPointConfigActivity.EXTRA_COUNT, clickCount)
            }
            startActivity(intent)
        }
        
        private fun updateClickPosition() {
            // 更新ClickSettings中的位置（圆圈中心）
            ClickSettings.clickX = (layoutParams.x + 25).toFloat() // 圆圈中心
            ClickSettings.clickY = (layoutParams.y + 25).toFloat()
            Log.d(TAG, "Updated click position to (${ClickSettings.clickX}, ${ClickSettings.clickY})")
        }

        fun getX(): Float = (layoutParams.x + 25).toFloat()
        fun getY(): Float = (layoutParams.y + 25).toFloat()
    }

    /**
     * 获取所有点击位置
     */
    fun getClickPoints(): List<ClickPointView> = clickPointViews.toList()
}
