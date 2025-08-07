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
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.autoclick.app.MainActivity
import com.autoclick.app.R
import com.autoclick.app.utils.ClickSettings

class FloatingWindowService : Service() {
    
    companion object {
        private const val TAG = "FloatingWindowService"
    }
    
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isViewAdded = false
    
    // UI组件
    private lateinit var btnStartStop: Button
    private lateinit var btnSettings: Button
    private lateinit var btnClose: ImageButton
    private lateinit var tvStatus: TextView
    private lateinit var tvClickCount: TextView
    
    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.autoclick.app.CLICK_STARTED" -> {
                    updateClickStatus(true)
                }
                "com.autoclick.app.CLICK_STOPPED" -> {
                    updateClickStatus(false)
                }
                "com.autoclick.app.CLICK_COUNT_UPDATED" -> {
                    val count = intent.getIntExtra("count", 0)
                    updateClickCount(count)
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingWindowService created")
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createFloatingWindow()
        registerServiceReceiver()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FloatingWindowService destroyed")
        
        removeFloatingWindow()
        unregisterReceiver(serviceReceiver)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createFloatingWindow() {
        try {
            // 创建悬浮窗视图
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null)
            
            // 初始化UI组件
            initViews()
            
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
            
            // 添加悬浮窗到窗口管理器
            windowManager?.addView(floatingView, layoutParams)
            isViewAdded = true
            
            // 设置拖拽功能
            setupDragListener(layoutParams)
            
            Log.d(TAG, "Floating window created successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create floating window", e)
        }
    }
    
    private fun initViews() {
        floatingView?.let { view ->
            btnStartStop = view.findViewById(R.id.btnFloatingStartStop)
            btnSettings = view.findViewById(R.id.btnFloatingSettings)
            btnClose = view.findViewById(R.id.btnClose)
            tvStatus = view.findViewById(R.id.tvFloatingStatus)
            tvClickCount = view.findViewById(R.id.tvFloatingClickCount)
            
            // 设置点击监听器
            btnStartStop.setOnClickListener {
                toggleAutoClick()
            }
            
            btnSettings.setOnClickListener {
                openMainActivity()
            }
            
            btnClose.setOnClickListener {
                stopSelf()
            }
            
            // 初始化状态
            updateUI()
        }
    }
    
    private fun setupDragListener(layoutParams: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        floatingView?.setOnTouchListener { _, event ->
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
                    windowManager?.updateViewLayout(floatingView, layoutParams)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun removeFloatingWindow() {
        try {
            if (isViewAdded && floatingView != null) {
                windowManager?.removeView(floatingView)
                isViewAdded = false
                floatingView = null
                Log.d(TAG, "Floating window removed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove floating window", e)
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
    
    private fun updateUI() {
        val service = AutoClickService.instance
        if (service != null) {
            updateClickStatus(service.isAutoClicking())
            updateClickCount(service.getClickCount())
        } else {
            updateClickStatus(false)
            updateClickCount(0)
        }
    }
    
    private fun updateClickStatus(isRunning: Boolean) {
        tvStatus.text = if (isRunning) {
            getString(R.string.status_running)
        } else {
            getString(R.string.status_stopped)
        }
        
        btnStartStop.text = if (isRunning) {
            getString(R.string.stop_click)
        } else {
            getString(R.string.start_click)
        }
    }
    
    private fun updateClickCount(count: Int) {
        tvClickCount.text = getString(R.string.click_count, count)
    }
    
    private fun registerServiceReceiver() {
        val filter = IntentFilter().apply {
            addAction("com.autoclick.app.CLICK_STARTED")
            addAction("com.autoclick.app.CLICK_STOPPED")
            addAction("com.autoclick.app.CLICK_COUNT_UPDATED")
        }
        registerReceiver(serviceReceiver, filter)
    }
}
