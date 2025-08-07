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
import android.view.*
import android.widget.Toast
import com.autoclick.app.MainActivity
import com.autoclick.app.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FloatingMiniControllerService : Service() {
    
    companion object {
        private const val TAG = "FloatingMiniController"
        var instance: FloatingMiniControllerService? = null
    }
    
    private var windowManager: WindowManager? = null
    private var miniView: View? = null
    private var isViewAdded = false
    
    // UI组件
    private lateinit var fabMiniStartStop: FloatingActionButton
    private lateinit var fabMiniDelete: FloatingActionButton
    private lateinit var fabMiniExpand: FloatingActionButton
    
    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.autoclick.app.CLICK_STARTED" -> {
                    updateStartStopButton(true)
                }
                "com.autoclick.app.CLICK_STOPPED" -> {
                    updateStartStopButton(false)
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingMiniControllerService created")
        instance = this

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createMiniController()
        registerServiceReceiver()

        // 通知管理器当前控制器类型
        FloatingControllerManager.setCurrentController(FloatingControllerManager.ControllerType.MINI_CONTROLLER)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FloatingMiniControllerService destroyed")

        removeMiniController()
        unregisterReceiver(serviceReceiver)
        instance = null

        // 通知管理器控制器已关闭
        if (FloatingControllerManager.getCurrentController() == FloatingControllerManager.ControllerType.MINI_CONTROLLER) {
            FloatingControllerManager.setCurrentController(FloatingControllerManager.ControllerType.NONE)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createMiniController() {
        try {
            // 创建最小化控制器视图
            miniView = LayoutInflater.from(this).inflate(R.layout.floating_mini_controller, null)
            
            // 初始化UI组件
            initViews()
            
            // 设置窗口参数
            val layoutParams = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                type = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                    }
                    else -> {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    }
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.END
                x = 20
                y = 200
            }
            
            // 添加最小化控制器到窗口管理器
            windowManager?.addView(miniView, layoutParams)
            isViewAdded = true
            
            // 设置拖拽功能
            setupDragListener(layoutParams)
            
            Log.d(TAG, "Floating mini controller created successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create floating mini controller", e)
        }
    }
    
    private fun initViews() {
        miniView?.let { view ->
            fabMiniStartStop = view.findViewById(R.id.fabMiniStartStop)
            fabMiniDelete = view.findViewById(R.id.fabMiniDelete)
            fabMiniExpand = view.findViewById(R.id.fabMiniExpand)
            
            // 设置点击监听器
            fabMiniStartStop.setOnClickListener {
                toggleAutoClick()
            }
            
            fabMiniDelete.setOnClickListener {
                deleteAllClickPoints()
            }
            
            fabMiniExpand.setOnClickListener {
                expandToFullPanel()
            }
            
            // 初始化状态
            updateStartStopButton(false)
        }
    }
    
    private fun setupDragListener(layoutParams: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        miniView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (initialTouchX - event.rawX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(miniView, layoutParams)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun removeMiniController() {
        try {
            if (isViewAdded && miniView != null) {
                windowManager?.removeView(miniView)
                isViewAdded = false
                miniView = null
                Log.d(TAG, "Floating mini controller removed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove floating mini controller", e)
        }
    }
    
    private fun toggleAutoClick() {
        val service = AutoClickService.instance
        if (service == null) {
            Log.w(TAG, "AutoClickService not available")
            Toast.makeText(this, "自动点击服务未启动", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (service.isAutoClicking()) {
            service.stopAutoClick()
            Toast.makeText(this, "已停止自动点击", Toast.LENGTH_SHORT).show()
        } else {
            service.startAutoClick()
            Toast.makeText(this, "已开始自动点击", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteAllClickPoints() {
        val intent = Intent(this, ClickPointService::class.java).apply {
            action = "REMOVE_ALL_POINTS"
        }
        startService(intent)
        Toast.makeText(this, "已清除所有点击位置", Toast.LENGTH_SHORT).show()
    }
    
    private fun expandToFullPanel() {
        // 使用管理器切换到完整控制面板
        FloatingControllerManager.switchToFullPanel(this)
    }
    
    private fun updateStartStopButton(isRunning: Boolean) {
        if (isRunning) {
            fabMiniStartStop.setImageResource(R.drawable.ic_pause)
        } else {
            fabMiniStartStop.setImageResource(R.drawable.ic_play)
        }
    }
    
    private fun registerServiceReceiver() {
        val filter = IntentFilter().apply {
            addAction("com.autoclick.app.CLICK_STARTED")
            addAction("com.autoclick.app.CLICK_STOPPED")
        }
        registerReceiver(serviceReceiver, filter)
    }
}
