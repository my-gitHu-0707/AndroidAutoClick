package com.autoclick.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.autoclick.app.databinding.ActivityMainBinding
import com.autoclick.app.service.AutoClickService
import com.autoclick.app.service.FloatingWindowService
import com.autoclick.app.service.ClickPointService
import com.autoclick.app.utils.ClickSettings
import com.autoclick.app.utils.PermissionUtils

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var isFloatingWindowShown = false
    
    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.autoclick.app.SERVICE_CONNECTED" -> {
                    updateUI()
                }
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化设置
        ClickSettings.init(this)
        
        // 设置监听器
        setupListeners()
        
        // 更新UI
        updateUI()
        
        // 注册广播接收器
        registerServiceReceiver()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(serviceReceiver)
    }
    
    override fun onResume() {
        super.onResume()
        updateUI()
    }
    
    private fun setupListeners() {
        // 无障碍设置按钮
        binding.btnAccessibilitySettings.setOnClickListener {
            Log.d("MainActivity", "Accessibility settings button clicked")
            Toast.makeText(this, "正在打开无障碍设置...", Toast.LENGTH_SHORT).show()
            PermissionUtils.openAccessibilitySettings(this)
        }

        // 悬浮窗设置按钮
        binding.btnOverlaySettings.setOnClickListener {
            Log.d("MainActivity", "Overlay settings button clicked")
            Toast.makeText(this, "正在打开悬浮窗设置...", Toast.LENGTH_SHORT).show()
            PermissionUtils.openOverlaySettings(this)
        }
        
        // 开始/停止按钮
        binding.btnStartStop.setOnClickListener {
            toggleAutoClick()
        }
        
        // 悬浮窗按钮
        binding.btnFloatingWindow.setOnClickListener {
            toggleFloatingWindow()
        }

        // 添加点击点按钮
        binding.btnAddClickPoint.setOnClickListener {
            addClickPoint()
        }

        // 管理点击点按钮
        binding.btnManagePoints.setOnClickListener {
            manageClickPoints()
        }
    }
    
    private fun updateUI() {
        // 更新权限状态
        updatePermissionStatus()
        
        // 更新按钮状态
        val hasAllPermissions = PermissionUtils.hasAllPermissions(this)
        binding.btnStartStop.isEnabled = hasAllPermissions
        binding.btnFloatingWindow.isEnabled = hasAllPermissions
        binding.btnAddClickPoint.isEnabled = hasAllPermissions
        binding.btnManagePoints.isEnabled = hasAllPermissions
        
        // 更新点击状态
        val service = AutoClickService.instance
        if (service != null) {
            updateClickStatus(service.isAutoClicking())
            updateClickCount(service.getClickCount())
        }
        
        // 加载保存的设置
        loadSettings()
    }
    
    private fun updatePermissionStatus() {
        // 无障碍权限状态
        val accessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(this)
        binding.tvAccessibilityStatus.text = if (accessibilityEnabled) {
            getString(R.string.permission_granted)
        } else {
            getString(R.string.permission_denied)
        }
        binding.tvAccessibilityStatus.setTextColor(
            ContextCompat.getColor(this, if (accessibilityEnabled) R.color.green else R.color.red)
        )
        
        // 悬浮窗权限状态
        val overlayEnabled = PermissionUtils.canDrawOverlays(this)
        binding.tvOverlayStatus.text = if (overlayEnabled) {
            getString(R.string.permission_granted)
        } else {
            getString(R.string.permission_denied)
        }
        binding.tvOverlayStatus.setTextColor(
            ContextCompat.getColor(this, if (overlayEnabled) R.color.green else R.color.red)
        )
    }
    
    private fun loadSettings() {
        binding.etClickInterval.setText(ClickSettings.clickInterval.toString())
        binding.etClickX.setText(ClickSettings.clickX.toString())
        binding.etClickY.setText(ClickSettings.clickY.toString())
    }
    
    private fun saveSettings() {
        try {
            val interval = binding.etClickInterval.text.toString().toLongOrNull() ?: 1000L
            val x = binding.etClickX.text.toString().toFloatOrNull() ?: 500f
            val y = binding.etClickY.text.toString().toFloatOrNull() ?: 500f
            
            ClickSettings.clickInterval = interval.coerceAtLeast(100L) // 最小间隔100ms
            ClickSettings.setClickPosition(x.coerceAtLeast(0f), y.coerceAtLeast(0f))
        } catch (e: Exception) {
            Toast.makeText(this, "设置保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun toggleAutoClick() {
        val service = AutoClickService.instance
        if (service == null) {
            Toast.makeText(this, "无障碍服务未连接", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (service.isAutoClicking()) {
            service.stopAutoClick()
        } else {
            // 保存设置
            saveSettings()
            service.startAutoClick()
        }
    }
    
    private fun toggleFloatingWindow() {
        if (isFloatingWindowShown) {
            stopService(Intent(this, FloatingWindowService::class.java))
            isFloatingWindowShown = false
            binding.btnFloatingWindow.text = getString(R.string.show_floating_window)
        } else {
            startService(Intent(this, FloatingWindowService::class.java))
            isFloatingWindowShown = true
            binding.btnFloatingWindow.text = getString(R.string.hide_floating_window)
        }
    }
    
    private fun updateClickStatus(isRunning: Boolean) {
        binding.tvStatus.text = if (isRunning) {
            getString(R.string.status_running)
        } else {
            getString(R.string.status_stopped)
        }
        
        binding.btnStartStop.text = if (isRunning) {
            getString(R.string.stop_click)
        } else {
            getString(R.string.start_click)
        }
    }
    
    private fun updateClickCount(count: Int) {
        binding.tvClickCount.text = getString(R.string.click_count, count)
    }
    
    private fun registerServiceReceiver() {
        val filter = IntentFilter().apply {
            addAction("com.autoclick.app.SERVICE_CONNECTED")
            addAction("com.autoclick.app.CLICK_STARTED")
            addAction("com.autoclick.app.CLICK_STOPPED")
            addAction("com.autoclick.app.CLICK_COUNT_UPDATED")
        }
        registerReceiver(serviceReceiver, filter)
    }

    private fun addClickPoint() {
        if (!PermissionUtils.canDrawOverlays(this)) {
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
            return
        }

        // 在屏幕中央添加一个点击点
        val intent = Intent(this, ClickPointService::class.java).apply {
            action = "ADD_CLICK_POINT"
            putExtra("x", 500f)
            putExtra("y", 800f)
        }
        startService(intent)
        Toast.makeText(this, "已添加点击位置，可拖拽移动，长按配置", Toast.LENGTH_LONG).show()
    }

    private fun manageClickPoints() {
        // 移除所有点击点
        val intent = Intent(this, ClickPointService::class.java).apply {
            action = "REMOVE_ALL_POINTS"
        }
        startService(intent)
        Toast.makeText(this, "已清除所有点击位置", Toast.LENGTH_SHORT).show()
    }
}
