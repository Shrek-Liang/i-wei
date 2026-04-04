package com.example.weatherapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherapp.ui.weather.WeatherDetailActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 直接跳转到天气详情页面
        val intent = Intent(this, WeatherDetailActivity::class.java)
        startActivity(intent)
        finish()
    }
}
