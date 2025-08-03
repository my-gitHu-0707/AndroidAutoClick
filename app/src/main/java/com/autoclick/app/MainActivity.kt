package com.autoclick.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.autoclick.app.databinding.ActivityMainBinding
import com.autoclick.app.service.AutoClickService
import com.autoclick.app.service.FloatingWindowService
import com.autoclick.app.service.MultiFloatingWindowService
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
            PermissionUtils.openAccessibilitySettings(this)
        }
        
        // 悬浮窗设置按钮
        binding.btnOverlaySettings.setOnClickListener {
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

        // 最小化按钮
        binding.btnMinimize.setOnClickListener {
            minimizeApp()
        }

        // 添加点击点按钮
        binding.btnAddClickPoint.setOnClickListener {
            addNewClickPoint()
        }

        // 管理点击点按钮
        binding.btnManagePoints.setOnClickListener {
            showClickPointsManager()
        }
    }
    
    private fun updateUI() {
        // 更新权限状态
        updatePermissionStatus()
        
        // 更新按钮状态
        val hasAllPermissions = PermissionUtils.hasAllPermissions(this)
        binding.btnStartStop.isEnabled = hasAllPermissions
        binding.btnFloatingWindow.isEnabled = hasAllPermissions
        binding.btnMinimize.isEnabled = hasAllPermissions
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
            stopService(Intent(this, MultiFloatingWindowService::class.java))
            isFloatingWindowShown = false
            binding.btnFloatingWindow.text = getString(R.string.show_floating_window)
        } else {
            startService(Intent(this, MultiFloatingWindowService::class.java))
            isFloatingWindowShown = true
            binding.btnFloatingWindow.text = getString(R.string.hide_floating_window)

            // 启动悬浮窗后可以选择最小化应用
            if (ClickSettings.isMinimized) {
                minimizeApp()
            }
        }
    }

    private fun minimizeApp() {
        // 最小化应用到后台
        moveTaskToBack(true)
        ClickSettings.isMinimized = true
    }

    private fun addNewClickPoint() {
        // 在屏幕中心添加新的点击点
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2f
        val centerY = displayMetrics.heightPixels / 2f

        val newPoint = ClickSettings.addClickPoint(centerX, centerY, ClickSettings.globalInterval)

        // 发送更新广播
        sendBroadcast(Intent("com.autoclick.app.CLICK_POINTS_UPDATED"))

        Toast.makeText(this, "已添加新的点击点 #${newPoint.id}", Toast.LENGTH_SHORT).show()

        // 如果悬浮窗正在显示，更新悬浮窗
        if (isFloatingWindowShown) {
            sendBroadcast(Intent("com.autoclick.app.CLICK_POINTS_UPDATED"))
        }
    }

    private fun showClickPointsManager() {
        // 显示点击点管理对话框
        val points = ClickSettings.clickPoints
        if (points.isEmpty()) {
            Toast.makeText(this, "暂无点击点", Toast.LENGTH_SHORT).show()
            return
        }

        val pointNames = points.map { "点击点 #${it.id} (${it.x.toInt()}, ${it.y.toInt()})" }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("管理点击点")
            .setItems(pointNames) { _, which ->
                val selectedPoint = points[which]
                showClickPointOptions(selectedPoint)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showClickPointOptions(point: ClickSettings.ClickPoint) {
        val options = arrayOf("编辑", "删除", "切换启用状态")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("点击点 #${point.id}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editClickPoint(point)
                    1 -> deleteClickPoint(point)
                    2 -> toggleClickPointEnabled(point)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun editClickPoint(point: ClickSettings.ClickPoint) {
        val intent = Intent(this, ClickPointConfigActivity::class.java)
        intent.putExtra("point_id", point.id)
        startActivity(intent)
    }

    private fun deleteClickPoint(point: ClickSettings.ClickPoint) {
        if (ClickSettings.clickPoints.size <= 1) {
            Toast.makeText(this, "至少需要保留一个点击点", Toast.LENGTH_SHORT).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("删除点击点")
            .setMessage("确定要删除点击点 #${point.id} 吗？")
            .setPositiveButton("删除") { _, _ ->
                ClickSettings.removeClickPoint(point.id)
                sendBroadcast(Intent("com.autoclick.app.CLICK_POINTS_UPDATED"))
                Toast.makeText(this, "点击点已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun toggleClickPointEnabled(point: ClickSettings.ClickPoint) {
        ClickSettings.updateClickPoint(point.id, isEnabled = !point.isEnabled)
        sendBroadcast(Intent("com.autoclick.app.CLICK_POINTS_UPDATED"))
        val status = if (point.isEnabled) "已禁用" else "已启用"
        Toast.makeText(this, "点击点 #${point.id} $status", Toast.LENGTH_SHORT).show()
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
}
