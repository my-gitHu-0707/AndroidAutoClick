package com.autoclick.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.autoclick.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // 基本的UI设置
        binding.btnStartStop.setOnClickListener {
            // TODO: 实现开始/停止功能
        }

        binding.btnFloatingWindow.setOnClickListener {
            // TODO: 实现悬浮窗功能
        }

        binding.btnAccessibilitySettings.setOnClickListener {
            // TODO: 实现无障碍设置
        }

        binding.btnOverlaySettings.setOnClickListener {
            // TODO: 实现悬浮窗权限设置
        }
    }
}
