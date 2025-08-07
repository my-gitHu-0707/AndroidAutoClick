package com.autoclick.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.autoclick.app.databinding.DialogClickPointConfigBinding

class ClickPointConfigActivity : AppCompatActivity() {
    
    private lateinit var binding: DialogClickPointConfigBinding
    
    companion object {
        const val EXTRA_INTERVAL = "interval"
        const val EXTRA_COUNT = "count"
        const val RESULT_INTERVAL = "result_interval"
        const val RESULT_COUNT = "result_count"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogClickPointConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 设置为对话框样式
        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        setupUI()
        setupListeners()
    }
    
    private fun setupUI() {
        // 获取传入的参数
        val currentInterval = intent.getLongExtra(EXTRA_INTERVAL, 1000L)
        val currentCount = intent.getIntExtra(EXTRA_COUNT, -1)
        
        // 设置当前值
        binding.etInterval.setText(currentInterval.toString())
        
        if (currentCount == -1) {
            binding.rbInfinite.isChecked = true
            binding.tilCustomCount.visibility = View.GONE
        } else {
            binding.rbCustomCount.isChecked = true
            binding.tilCustomCount.visibility = View.VISIBLE
            binding.etCustomCount.setText(currentCount.toString())
        }
    }
    
    private fun setupListeners() {
        // 点击次数选择监听
        binding.rgClickCount.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbInfinite -> {
                    binding.tilCustomCount.visibility = View.GONE
                }
                R.id.rbCustomCount -> {
                    binding.tilCustomCount.visibility = View.VISIBLE
                }
            }
        }
        
        // 取消按钮
        binding.btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        
        // 确定按钮
        binding.btnConfirm.setOnClickListener {
            saveConfiguration()
        }
    }
    
    private fun saveConfiguration() {
        try {
            // 获取间隔时间
            val intervalText = binding.etInterval.text.toString()
            val interval = if (intervalText.isNotEmpty()) {
                maxOf(intervalText.toLong(), 100L) // 最小100ms
            } else {
                1000L
            }
            
            // 获取点击次数
            val count = if (binding.rbInfinite.isChecked) {
                -1 // 无限次
            } else {
                val countText = binding.etCustomCount.text.toString()
                if (countText.isNotEmpty()) {
                    maxOf(countText.toInt(), 1) // 最小1次
                } else {
                    10
                }
            }
            
            // 发送广播通知配置更新
            val broadcastIntent = Intent("com.autoclick.app.CLICK_POINT_CONFIG_UPDATED").apply {
                putExtra(RESULT_INTERVAL, interval)
                putExtra(RESULT_COUNT, count)
            }
            sendBroadcast(broadcastIntent)

            // 返回结果
            val resultIntent = Intent().apply {
                putExtra(RESULT_INTERVAL, interval)
                putExtra(RESULT_COUNT, count)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
            
        } catch (e: NumberFormatException) {
            // 输入格式错误，使用默认值
            val broadcastIntent = Intent("com.autoclick.app.CLICK_POINT_CONFIG_UPDATED").apply {
                putExtra(RESULT_INTERVAL, 1000L)
                putExtra(RESULT_COUNT, -1)
            }
            sendBroadcast(broadcastIntent)

            val resultIntent = Intent().apply {
                putExtra(RESULT_INTERVAL, 1000L)
                putExtra(RESULT_COUNT, -1)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}
