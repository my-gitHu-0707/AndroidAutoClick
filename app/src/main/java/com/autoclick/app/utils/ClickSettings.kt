package com.autoclick.app.utils

import android.content.Context
import android.content.SharedPreferences

object ClickSettings {
    private const val PREFS_NAME = "click_settings"
    private const val KEY_CLICK_X = "click_x"
    private const val KEY_CLICK_Y = "click_y"
    private const val KEY_CLICK_INTERVAL = "click_interval"
    
    private var prefs: SharedPreferences? = null
    
    // 默认值
    var clickX: Float = 500f
        get() = prefs?.getFloat(KEY_CLICK_X, 500f) ?: field
        set(value) {
            field = value
            prefs?.edit()?.putFloat(KEY_CLICK_X, value)?.apply()
        }
    
    var clickY: Float = 500f
        get() = prefs?.getFloat(KEY_CLICK_Y, 500f) ?: field
        set(value) {
            field = value
            prefs?.edit()?.putFloat(KEY_CLICK_Y, value)?.apply()
        }
    
    var clickInterval: Long = 1000L
        get() = prefs?.getLong(KEY_CLICK_INTERVAL, 1000L) ?: field
        set(value) {
            field = value
            prefs?.edit()?.putLong(KEY_CLICK_INTERVAL, value)?.apply()
        }
    
    /**
     * 初始化设置
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // 加载保存的值
        clickX = prefs!!.getFloat(KEY_CLICK_X, 500f)
        clickY = prefs!!.getFloat(KEY_CLICK_Y, 500f)
        clickInterval = prefs!!.getLong(KEY_CLICK_INTERVAL, 1000L)
    }
    
    /**
     * 设置点击坐标
     */
    fun setClickPosition(x: Float, y: Float) {
        clickX = x
        clickY = y
    }
    
    /**
     * 获取点击坐标
     */
    fun getClickPosition(): Pair<Float, Float> {
        return Pair(clickX, clickY)
    }
}
