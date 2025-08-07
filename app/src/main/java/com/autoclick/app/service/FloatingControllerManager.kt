package com.autoclick.app.service

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 悬浮控制器管理器
 * 负责协调完整控制面板和最小化控制器之间的切换
 */
object FloatingControllerManager {
    
    private const val TAG = "FloatingControllerManager"
    
    enum class ControllerType {
        NONE,
        FULL_PANEL,
        MINI_CONTROLLER
    }
    
    private var currentController: ControllerType = ControllerType.NONE
    
    /**
     * 显示完整控制面板
     */
    fun showFullPanel(context: Context) {
        Log.d(TAG, "Showing full panel")
        
        // 先关闭最小化控制器
        if (currentController == ControllerType.MINI_CONTROLLER) {
            stopMiniController(context)
        }
        
        // 启动完整控制面板
        val intent = Intent(context, FloatingControlPanelService::class.java)
        context.startService(intent)
        
        currentController = ControllerType.FULL_PANEL
    }
    
    /**
     * 显示最小化控制器
     */
    fun showMiniController(context: Context) {
        Log.d(TAG, "Showing mini controller")
        
        // 先关闭完整控制面板
        if (currentController == ControllerType.FULL_PANEL) {
            stopFullPanel(context)
        }
        
        // 启动最小化控制器
        val intent = Intent(context, FloatingMiniControllerService::class.java)
        context.startService(intent)
        
        currentController = ControllerType.MINI_CONTROLLER
    }
    
    /**
     * 关闭所有控制器
     */
    fun hideAllControllers(context: Context) {
        Log.d(TAG, "Hiding all controllers")
        
        stopFullPanel(context)
        stopMiniController(context)
        
        currentController = ControllerType.NONE
    }
    
    /**
     * 切换到完整控制面板
     */
    fun switchToFullPanel(context: Context) {
        if (currentController != ControllerType.FULL_PANEL) {
            showFullPanel(context)
        }
    }
    
    /**
     * 切换到最小化控制器
     */
    fun switchToMiniController(context: Context) {
        if (currentController != ControllerType.MINI_CONTROLLER) {
            showMiniController(context)
        }
    }
    
    /**
     * 获取当前控制器类型
     */
    fun getCurrentController(): ControllerType {
        return currentController
    }
    
    /**
     * 设置当前控制器类型（由服务调用）
     */
    fun setCurrentController(type: ControllerType) {
        currentController = type
        Log.d(TAG, "Current controller set to: $type")
    }
    
    private fun stopFullPanel(context: Context) {
        val intent = Intent(context, FloatingControlPanelService::class.java)
        context.stopService(intent)
    }
    
    private fun stopMiniController(context: Context) {
        val intent = Intent(context, FloatingMiniControllerService::class.java)
        context.stopService(intent)
    }
}
