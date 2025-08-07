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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.autoclick.app.MainActivity
import com.autoclick.app.R
import com.autoclick.app.utils.PermissionUtils

class FloatingControlPanelService : Service() {
    
    companion object {
        private const val TAG = "FloatingControlPanel"
        var instance: FloatingControlPanelService? = null
    }
    
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isViewAdded = false
    private var isExpanded = true
    
    // UI组件
    private lateinit var layoutExecute: LinearLayout
    private lateinit var layoutAdd: LinearLayout
    private lateinit var layoutDelete: LinearLayout
    private lateinit var layoutList: LinearLayout
    private lateinit var layoutHide: LinearLayout
    private lateinit var layoutSettings: LinearLayout
    
    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.autoclick.app.CLICK_STARTED" -> {
                    updateExecuteButton(true)
                }
                "com.autoclick.app.CLICK_STOPPED" -> {
                    updateExecuteButton(false)
                }
                "com.autoclick.app.CLICK_POINTS_CHANGED" -> {
                    updateExecuteButtonState()
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingControlPanelService created")
        instance = this

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createFloatingPanel()
        registerServiceReceiver()

        // 通知管理器当前控制器类型
        FloatingControllerManager.setCurrentController(FloatingControllerManager.ControllerType.FULL_PANEL)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FloatingControlPanelService destroyed")

        removeFloatingPanel()
        unregisterReceiver(serviceReceiver)
        instance = null

        // 通知管理器控制器已关闭
        if (FloatingControllerManager.getCurrentController() == FloatingControllerManager.ControllerType.FULL_PANEL) {
            FloatingControllerManager.setCurrentController(FloatingControllerManager.ControllerType.NONE)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createFloatingPanel() {
        try {
            Log.d(TAG, "Starting to create floating control panel...")

            // 检查悬浮窗权限
            if (!PermissionUtils.canDrawOverlays(this)) {
                Log.e(TAG, "No overlay permission granted")
                Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_LONG).show()
                stopSelf()
                return
            }

            // 创建悬浮面板视图
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_control_panel, null)
            Log.d(TAG, "Floating view inflated successfully")

            // 初始化UI组件
            initViews()
            Log.d(TAG, "UI components initialized")

            // 设置窗口参数
            val layoutParams = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                type = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        Log.d(TAG, "Using TYPE_APPLICATION_OVERLAY for Android O+")
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                        Log.d(TAG, "Using TYPE_SYSTEM_ALERT for Android M+")
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                    }
                    else -> {
                        Log.d(TAG, "Using TYPE_PHONE for older Android")
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    }
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.START
                x = 50
                y = 200
            }

            Log.d(TAG, "Window layout params configured")

            // 添加悬浮面板到窗口管理器
            windowManager?.addView(floatingView, layoutParams)
            isViewAdded = true

            Log.d(TAG, "Floating view added to window manager")

            // 设置拖拽功能
            setupDragListener(layoutParams)

            Log.d(TAG, "Floating control panel created successfully")
            Toast.makeText(this, "悬浮控制面板已显示", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create floating control panel", e)
            Toast.makeText(this, "悬浮面板创建失败: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }
    
    private fun initViews() {
        floatingView?.let { view ->
            layoutExecute = view.findViewById(R.id.layoutExecute)
            layoutAdd = view.findViewById(R.id.layoutAdd)
            layoutDelete = view.findViewById(R.id.layoutDelete)
            layoutList = view.findViewById(R.id.layoutList)
            layoutHide = view.findViewById(R.id.layoutHide)
            layoutSettings = view.findViewById(R.id.layoutSettings)

            // 设置点击监听器 - 开始/停止按钮最高优先级
            layoutExecute.setOnClickListener {
                // 立即响应，不管当前状态如何
                Log.d(TAG, "Execute button clicked - immediate response")
                toggleAutoClick()
            }

            // 设置多重点击监听，确保响应性
            layoutExecute.setOnTouchListener { _, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        Log.d(TAG, "Execute button touch down - preparing immediate response")
                        true
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        Log.d(TAG, "Execute button touch up - triggering action")
                        toggleAutoClick()
                        true
                    }
                    else -> false
                }
            }
            
            layoutAdd.setOnClickListener {
                showAddOptions()
            }
            
            layoutDelete.setOnClickListener {
                deleteAllClickPoints()
            }
            
            layoutList.setOnClickListener {
                openMainActivity()
            }
            
            layoutHide.setOnClickListener {
                hidePanel()
            }

            layoutSettings.setOnClickListener {
                openMainActivity()
            }
            
            // 设置长按监听器（关闭面板）
            view.setOnLongClickListener {
                stopSelf()
                true
            }
            
            // 初始化状态
            updateExecuteButton(false)
            updateExecuteButtonState()
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
    
    private fun removeFloatingPanel() {
        try {
            if (isViewAdded && floatingView != null) {
                windowManager?.removeView(floatingView)
                isViewAdded = false
                floatingView = null
                Log.d(TAG, "Floating control panel removed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove floating control panel", e)
        }
    }
    
    private fun toggleAutoClick() {
        Log.d(TAG, "toggleAutoClick called - checking service")
        val service = AutoClickService.instance
        if (service == null) {
            Log.w(TAG, "AutoClickService not available")
            Toast.makeText(this, "无障碍服务未启用", Toast.LENGTH_SHORT).show()
            return
        }

        val isCurrentlyClicking = service.isAutoClicking()
        Log.d(TAG, "Current clicking state: $isCurrentlyClicking")

        if (isCurrentlyClicking) {
            // 停止操作 - 最高优先级，立即执行
            Log.d(TAG, "Stopping auto click - IMMEDIATE PRIORITY")
            service.stopAutoClick()
            // 立即更新UI状态
            updateExecuteButton(false)
            Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show()
        } else {
            // 检查是否有点击位置
            val clickPointService = ClickPointService.instance
            if (clickPointService == null || !clickPointService.hasClickPoints()) {
                Log.w(TAG, "No click points available")
                Toast.makeText(this, "请先添加点击位置", Toast.LENGTH_SHORT).show()
                return
            }

            // 开始操作
            Log.d(TAG, "Starting auto click with ${clickPointService.getClickPointCount()} points")
            service.startAutoClick()
            // 立即更新UI状态
            updateExecuteButton(true)
            Toast.makeText(this, "已开始 (${clickPointService.getClickPointCount()}个位置)", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showAddOptions() {
        Log.d(TAG, "showAddOptions called")
        try {
            // 启动添加功能选择器
            val intent = Intent(this, AddActionSelectorService::class.java)
            startService(intent)
            Log.d(TAG, "AddActionSelectorService started")
            Toast.makeText(this, "正在显示添加选项...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AddActionSelectorService", e)
            Toast.makeText(this, "启动添加选项失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteAllClickPoints() {
        val intent = Intent(this, ClickPointService::class.java).apply {
            action = "REMOVE_ALL_POINTS"
        }
        startService(intent)
        Toast.makeText(this, "已清除所有点击位置", Toast.LENGTH_SHORT).show()
    }
    
    private fun hidePanel() {
        // 使用管理器切换到最小化控制器
        FloatingControllerManager.switchToMiniController(this)
    }
    
    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }
    
    private fun updateExecuteButton(isRunning: Boolean) {
        // 找到执行按钮中的ImageView并更新图标
        val executeImageView = layoutExecute.getChildAt(0) as? ImageView
        if (isRunning) {
            executeImageView?.setImageResource(R.drawable.ic_pause)
        } else {
            executeImageView?.setImageResource(R.drawable.ic_play)
        }

        // 检查是否有点击位置，动态启用/禁用按钮
        updateExecuteButtonState()
    }

    private fun updateExecuteButtonState() {
        val clickPointService = ClickPointService.instance
        val hasClickPoints = clickPointService?.hasClickPoints() ?: false
        val isRunning = AutoClickService.instance?.isAutoClicking() ?: false

        // 如果正在运行，按钮始终可用（用于停止）
        // 如果没有运行，只有在有点击位置时才可用
        val shouldEnable = isRunning || hasClickPoints

        layoutExecute.isEnabled = shouldEnable
        layoutExecute.alpha = if (shouldEnable) 1.0f else 0.5f

        Log.d(TAG, "Execute button state: enabled=$shouldEnable, hasPoints=$hasClickPoints, isRunning=$isRunning")
    }
    
    private fun registerServiceReceiver() {
        val filter = IntentFilter().apply {
            addAction("com.autoclick.app.CLICK_STARTED")
            addAction("com.autoclick.app.CLICK_STOPPED")
            addAction("com.autoclick.app.CLICK_POINTS_CHANGED")
        }
        registerReceiver(serviceReceiver, filter)
    }
}
