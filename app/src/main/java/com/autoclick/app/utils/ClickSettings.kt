package com.autoclick.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class ClickPoint(
    val id: Int,
    var x: Float,
    var y: Float,
    var interval: Long,
    var isEnabled: Boolean = true
)

object ClickSettings {
    private const val PREFS_NAME = "click_settings"
    private const val KEY_CLICK_POINTS = "click_points"
    private const val KEY_GLOBAL_INTERVAL = "global_interval"
    private const val KEY_IS_MINIMIZED = "is_minimized"

    private var prefs: SharedPreferences? = null
    private val gson = Gson()

    // 点击点列表
    private var _clickPoints = mutableListOf<ClickPoint>()
    val clickPoints: List<ClickPoint> get() = _clickPoints.toList()

    // 全局间隔（兼容旧版本）
    var globalInterval: Long = 1000L
        get() = prefs?.getLong(KEY_GLOBAL_INTERVAL, 1000L) ?: field
        set(value) {
            field = value
            prefs?.edit()?.putLong(KEY_GLOBAL_INTERVAL, value)?.apply()
        }

    // 应用是否最小化
    var isMinimized: Boolean = false
        get() = prefs?.getBoolean(KEY_IS_MINIMIZED, false) ?: field
        set(value) {
            field = value
            prefs?.edit()?.putBoolean(KEY_IS_MINIMIZED, value)?.apply()
        }

    // 兼容旧版本的属性
    var clickX: Float
        get() = if (_clickPoints.isNotEmpty()) _clickPoints[0].x else 500f
        set(value) {
            if (_clickPoints.isEmpty()) {
                addClickPoint(value, 500f, globalInterval)
            } else {
                _clickPoints[0].x = value
                saveClickPoints()
            }
        }

    var clickY: Float
        get() = if (_clickPoints.isNotEmpty()) _clickPoints[0].y else 500f
        set(value) {
            if (_clickPoints.isEmpty()) {
                addClickPoint(500f, value, globalInterval)
            } else {
                _clickPoints[0].y = value
                saveClickPoints()
            }
        }

    var clickInterval: Long
        get() = if (_clickPoints.isNotEmpty()) _clickPoints[0].interval else globalInterval
        set(value) {
            globalInterval = value
            if (_clickPoints.isNotEmpty()) {
                _clickPoints[0].interval = value
                saveClickPoints()
            }
        }

    /**
     * 初始化设置
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadClickPoints()

        // 如果没有点击点，创建默认的
        if (_clickPoints.isEmpty()) {
            addClickPoint(500f, 500f, globalInterval)
        }
    }

    /**
     * 添加点击点
     */
    fun addClickPoint(x: Float, y: Float, interval: Long): ClickPoint {
        val newId = (_clickPoints.maxOfOrNull { it.id } ?: 0) + 1
        val point = ClickPoint(newId, x, y, interval)
        _clickPoints.add(point)
        saveClickPoints()
        return point
    }

    /**
     * 删除点击点
     */
    fun removeClickPoint(id: Int) {
        _clickPoints.removeAll { it.id == id }
        saveClickPoints()
    }

    /**
     * 更新点击点
     */
    fun updateClickPoint(id: Int, x: Float? = null, y: Float? = null, interval: Long? = null, isEnabled: Boolean? = null) {
        val point = _clickPoints.find { it.id == id } ?: return
        x?.let { point.x = it }
        y?.let { point.y = it }
        interval?.let { point.interval = it }
        isEnabled?.let { point.isEnabled = it }
        saveClickPoints()
    }

    /**
     * 获取启用的点击点
     */
    fun getEnabledClickPoints(): List<ClickPoint> {
        return _clickPoints.filter { it.isEnabled }
    }

    /**
     * 保存点击点到SharedPreferences
     */
    private fun saveClickPoints() {
        val json = gson.toJson(_clickPoints)
        prefs?.edit()?.putString(KEY_CLICK_POINTS, json)?.apply()
    }

    /**
     * 从SharedPreferences加载点击点
     */
    private fun loadClickPoints() {
        val json = prefs?.getString(KEY_CLICK_POINTS, null)
        if (json != null) {
            try {
                val type = object : TypeToken<MutableList<ClickPoint>>() {}.type
                _clickPoints = gson.fromJson(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                _clickPoints = mutableListOf()
            }
        }
    }

    /**
     * 设置点击坐标（兼容旧版本）
     */
    fun setClickPosition(x: Float, y: Float) {
        clickX = x
        clickY = y
    }

    /**
     * 获取点击坐标（兼容旧版本）
     */
    fun getClickPosition(): Pair<Float, Float> {
        return Pair(clickX, clickY)
    }

    /**
     * 清空所有点击点
     */
    fun clearAllClickPoints() {
        _clickPoints.clear()
        saveClickPoints()
    }
}
