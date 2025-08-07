package com.autoclick.app

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.autoclick.app.utils.ClickSettings
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class ClickPointConfigActivity : AppCompatActivity() {
    
    private var pointId: Int = -1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        pointId = intent.getIntExtra("point_id", -1)
        if (pointId == -1) {
            finish()
            return
        }
        
        showConfigDialog()
    }
    
    private fun showConfigDialog() {
        val point = ClickSettings.clickPoints.find { it.id == pointId }
        if (point == null) {
            finish()
            return
        }
        
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_click_point_config)
        dialog.setCancelable(true)
        
        // 初始化视图
        val tvPointNumber = dialog.findViewById<TextView>(R.id.tvPointNumber)
        val etPointX = dialog.findViewById<TextInputEditText>(R.id.etPointX)
        val etPointY = dialog.findViewById<TextInputEditText>(R.id.etPointY)
        val etPointInterval = dialog.findViewById<TextInputEditText>(R.id.etPointInterval)
        val switchEnabled = dialog.findViewById<SwitchMaterial>(R.id.switchEnabled)
        val btnDelete = dialog.findViewById<Button>(R.id.btnDelete)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        
        // 设置当前值
        tvPointNumber.text = "点击点 #${point.id}"
        etPointX.setText(point.x.toString())
        etPointY.setText(point.y.toString())
        etPointInterval.setText(point.interval.toString())
        switchEnabled.isChecked = point.isEnabled
        
        // 设置按钮监听器
        btnDelete.setOnClickListener {
            deleteClickPoint()
            dialog.dismiss()
            finish()
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
            finish()
        }
        
        btnSave.setOnClickListener {
            saveClickPoint(etPointX, etPointY, etPointInterval, switchEnabled)
            dialog.dismiss()
            finish()
        }
        
        dialog.setOnDismissListener {
            finish()
        }
        
        dialog.show()
    }
    
    private fun saveClickPoint(
        etPointX: TextInputEditText,
        etPointY: TextInputEditText,
        etPointInterval: TextInputEditText,
        switchEnabled: SwitchMaterial
    ) {
        try {
            val x = etPointX.text.toString().toFloatOrNull() ?: 0f
            val y = etPointY.text.toString().toFloatOrNull() ?: 0f
            val interval = etPointInterval.text.toString().toLongOrNull() ?: 1000L
            val isEnabled = switchEnabled.isChecked
            
            // 验证输入
            if (x < 0 || y < 0) {
                Toast.makeText(this, "坐标不能为负数", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (interval < 100) {
                Toast.makeText(this, "间隔时间不能少于100毫秒", Toast.LENGTH_SHORT).show()
                return
            }
            
            // 更新点击点
            ClickSettings.updateClickPoint(pointId, x, y, interval, isEnabled)
            
            // 发送更新广播
            sendBroadcast(Intent("com.autoclick.app.CLICK_POINTS_UPDATED"))
            
            Toast.makeText(this, "点击点已更新", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteClickPoint() {
        // 如果只有一个点击点，不允许删除
        if (ClickSettings.clickPoints.size <= 1) {
            Toast.makeText(this, "至少需要保留一个点击点", Toast.LENGTH_SHORT).show()
            return
        }
        
        ClickSettings.removeClickPoint(pointId)
        
        // 发送更新广播
        sendBroadcast(Intent("com.autoclick.app.CLICK_POINTS_UPDATED"))
        
        Toast.makeText(this, "点击点已删除", Toast.LENGTH_SHORT).show()
    }
}
