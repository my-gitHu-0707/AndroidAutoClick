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
import android.widget.ImageButton
import com.autoclick.app.MainActivity
import com.autoclick.app.R
import com.autoclick.app.utils.ClickPoint
import com.autoclick.app.utils.ClickSettings

class MultiFloatingWindowService : Service() {
    
    companion object {
        private const val TAG = "MultiFloatingWindowService"
    }
    
    private var windowManager: WindowManager? = null
    private var miniControlView: View? = null
    private val clickCircleViews = mutableMapOf<Int, View>()
    private var isViewsAdded = false
    
    // 控制面板组件
    private lateinit var btnMiniStartStop: ImageButton
    private lateinit var btnExpand: ImageButton
    private lateinit var btnAddPoint: ImageButton
    
    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.autoclick.app.CLICK_STARTED" -> {
                    updateStartStopButton(true)
                }
                "com.autoclick.app.CLICK_STOPPED" -> {
                    updateStartStopButton(false)
                }
                "com.autoclick.app.CLICK_POINTS_UPDATED" -> {
                    updateClickCircles()
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MultiFloatingWindowService created")
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createFloatingViews()
        registerServiceReceiver()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MultiFloatingWindowService destroyed")
        
        removeFloatingViews()
        unregisterReceiver(serviceReceiver)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createFloatingViews() {
        try {
            createMiniControl()
            updateClickCircles()
            Log.d(TAG, "Floating views created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create floating views", e)
        }
    }
    
    private fun createMiniControl() {
        // 创建最小化控制面板
        miniControlView = LayoutInflater.from(this).inflate(R.layout.floating_mini_control, null)
        
        // 初始化控制组件
        initMiniControlViews()
        
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
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }
        
        // 添加到窗口管理器
        windowManager?.addView(miniControlView, layoutParams)
        isViewsAdded = true
        
        // 设置拖拽功能
        setupDragListener(miniControlView!!, layoutParams)
    }
    
    private fun initMiniControlViews() {
        miniControlView?.let { view ->
            btnMiniStartStop = view.findViewById(R.id.btnMiniStartStop)
            btnExpand = view.findViewById(R.id.btnExpand)
            btnAddPoint = view.findViewById(R.id.btnAddPoint)
            
            // 设置点击监听器
            btnMiniStartStop.setOnClickListener {
                toggleAutoClick()
            }
            
            btnExpand.setOnClickListener {
                openMainActivity()
            }
            
            btnAddPoint.setOnClickListener {
                addNewClickPoint()
            }
            
            // 初始化按钮状态
            updateStartStopButton(false)
        }
    }
    
    private fun updateClickCircles() {
        // 移除所有现有的圆圈
        clickCircleViews.values.forEach { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove click circle view", e)
            }
        }
        clickCircleViews.clear()

        // 为每个点击点创建圆圈
        ClickSettings.clickPoints.forEach { point ->
            createClickCircle(point)
        }
    }

    private fun createClickCircle(point: ClickPoint) {
        try {
            val circleView = LayoutInflater.from(this).inflate(R.layout.floating_click_circle, null)

            // 设置圆圈内容
            val tvNumber = circleView.findViewById<android.widget.TextView>(R.id.tvNumber)
            val enabledIndicator = circleView.findViewById<View>(R.id.enabledIndicator)

            tvNumber.text = point.id.toString()
            enabledIndicator.visibility = if (point.isEnabled) View.VISIBLE else View.GONE

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
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.START
                x = point.x.toInt() - 30 // 圆圈宽度的一半
                y = point.y.toInt() - 30 // 圆圈高度的一半
            }

            // 添加到窗口管理器
            windowManager?.addView(circleView, layoutParams)
            clickCircleViews[point.id] = circleView

            // 设置长按监听器
            setupCircleLongClickListener(circleView, point, layoutParams)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create click circle for point ${point.id}", e)
        }
    }

    private fun setupCircleLongClickListener(circleView: View, point: ClickPoint, layoutParams: WindowManager.LayoutParams) {
        var isLongClick = false
        var startTime = 0L

        circleView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isLongClick = false
                    startTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - startTime
                    if (duration > 500) { // 长按500ms
                        showClickPointConfigDialog(point)
                        isLongClick = true
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupDragListener(view: View, layoutParams: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(view, layoutParams)
                    true
                }
                else -> false
            }
        }
    }

    private fun removeFloatingViews() {
        try {
            // 移除控制面板
            if (isViewsAdded && miniControlView != null) {
                windowManager?.removeView(miniControlView)
                miniControlView = null
            }

            // 移除所有圆圈
            clickCircleViews.values.forEach { view ->
                windowManager?.removeView(view)
            }
            clickCircleViews.clear()

            isViewsAdded = false
            Log.d(TAG, "Floating views removed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove floating views", e)
        }
    }

    private fun toggleAutoClick() {
        val service = AutoClickService.instance
        if (service == null) {
            Log.w(TAG, "AutoClickService not available")
            return
        }

        if (service.isAutoClicking()) {
            service.stopAutoClick()
        } else {
            service.startAutoClick()
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun addNewClickPoint() {
        // 在屏幕中心添加新的点击点
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2f
        val centerY = displayMetrics.heightPixels / 2f

        val newPoint = ClickSettings.addClickPoint(centerX, centerY, ClickSettings.globalInterval)
        createClickCircle(newPoint)

        // 发送更新广播
        sendBroadcast(Intent("com.autoclick.app.CLICK_POINTS_UPDATED"))
    }

    private fun showClickPointConfigDialog(point: ClickPoint) {
        // 启动配置Activity
        val intent = Intent(this, ClickPointConfigActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("point_id", point.id)
        }
        startActivity(intent)
    }

    private fun updateStartStopButton(isRunning: Boolean) {
        val iconRes = if (isRunning) R.drawable.ic_pause else R.drawable.ic_play
        btnMiniStartStop.setImageResource(iconRes)
    }

    private fun registerServiceReceiver() {
        val filter = IntentFilter().apply {
            addAction("com.autoclick.app.CLICK_STARTED")
            addAction("com.autoclick.app.CLICK_STOPPED")
            addAction("com.autoclick.app.CLICK_POINTS_UPDATED")
        }
        registerReceiver(serviceReceiver, filter)
    }
}
