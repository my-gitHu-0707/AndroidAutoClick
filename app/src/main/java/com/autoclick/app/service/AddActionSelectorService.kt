package com.autoclick.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Toast
import com.autoclick.app.R
import android.widget.LinearLayout

class AddActionSelectorService : Service() {
    
    companion object {
        private const val TAG = "AddActionSelector"
        private const val AUTO_HIDE_DELAY = 5000L // 5秒后自动隐藏
    }
    
    private var windowManager: WindowManager? = null
    private var selectorView: View? = null
    private var isViewAdded = false
    private val autoHideHandler = Handler(Looper.getMainLooper())
    
    // UI组件
    private lateinit var cardDoubleClick: LinearLayout
    private lateinit var cardLongPress: LinearLayout
    private lateinit var cardSwipe: LinearLayout
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AddActionSelectorService created")

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 检查悬浮窗权限
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Log.e(TAG, "No overlay permission")
            Toast.makeText(this, "需要悬浮窗权限", Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        }

        createSelector()

        // 设置自动隐藏
        autoHideHandler.postDelayed({
            Log.d(TAG, "Auto hiding selector")
            stopSelf()
        }, AUTO_HIDE_DELAY)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AddActionSelectorService destroyed")
        
        removeSelector()
        autoHideHandler.removeCallbacksAndMessages(null)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createSelector() {
        try {
            Log.d(TAG, "Creating selector view...")

            // 创建选择器视图
            selectorView = LayoutInflater.from(this).inflate(R.layout.add_action_selector, null)
            Log.d(TAG, "Selector view inflated successfully")

            // 初始化UI组件
            initViews()
            Log.d(TAG, "Views initialized")

            // 设置窗口参数（居中显示）
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
                gravity = Gravity.CENTER
            }

            Log.d(TAG, "Adding view to window manager...")
            // 添加选择器到窗口管理器
            windowManager?.addView(selectorView, layoutParams)
            isViewAdded = true

            Log.d(TAG, "Add action selector created successfully")
            Toast.makeText(this, "选择器已显示", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create add action selector", e)
            Toast.makeText(this, "创建选择器失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun initViews() {
        selectorView?.let { view ->
            cardDoubleClick = view.findViewById(R.id.cardDoubleClick)
            cardLongPress = view.findViewById(R.id.cardLongPress)
            cardSwipe = view.findViewById(R.id.cardSwipe)
            
            // 设置点击监听器
            cardDoubleClick.setOnClickListener {
                addDoubleClickAction()
                stopSelf()
            }
            
            cardLongPress.setOnClickListener {
                addLongPressAction()
                stopSelf()
            }
            
            cardSwipe.setOnClickListener {
                addSwipeAction()
                stopSelf()
            }
            
            // 点击外部区域关闭
            view.setOnClickListener {
                stopSelf()
            }
        }
    }
    
    private fun removeSelector() {
        try {
            if (isViewAdded && selectorView != null) {
                windowManager?.removeView(selectorView)
                isViewAdded = false
                selectorView = null
                Log.d(TAG, "Add action selector removed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove add action selector", e)
        }
    }
    
    private fun addDoubleClickAction() {
        // 添加双击点击点
        val intent = Intent(this, ClickPointService::class.java).apply {
            action = "ADD_CLICK_POINT"
            putExtra("x", 500f)
            putExtra("y", 800f)
            putExtra("type", "double_click")
        }
        startService(intent)
        Toast.makeText(this, "已添加双击位置，可拖拽移动", Toast.LENGTH_SHORT).show()
    }
    
    private fun addLongPressAction() {
        // 添加长按点击点
        val intent = Intent(this, ClickPointService::class.java).apply {
            action = "ADD_CLICK_POINT"
            putExtra("x", 500f)
            putExtra("y", 800f)
            putExtra("type", "long_press")
        }
        startService(intent)
        Toast.makeText(this, "已添加长按位置，可拖拽移动", Toast.LENGTH_SHORT).show()
    }
    
    private fun addSwipeAction() {
        // 添加滑动轨迹
        Toast.makeText(this, "滑动轨迹功能开发中...", Toast.LENGTH_LONG).show()
        // TODO: 实现滑动轨迹功能
        // val intent = Intent(this, SwipeTrackService::class.java)
        // startService(intent)
    }
}
