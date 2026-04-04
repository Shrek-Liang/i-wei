package com.example.weatherapp.ui.weather

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.data.model.DailyForecast
import com.example.weatherapp.databinding.ItemForecastBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ForecastAdapter(
    private val forecasts: List<DailyForecast>
) : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {
    
    inner class ForecastViewHolder(private val binding: ItemForecastBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(forecast: DailyForecast) {
            // 格式化日期
            val date = try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(forecast.date)
            } catch (e: Exception) {
                null
            }
            
            val dayOfWeek = date?.let {
                SimpleDateFormat("E", Locale.getDefault()).format(it)
            } ?: forecast.date
            
            binding.dateTextView.text = dayOfWeek
            binding.conditionTextView.text = forecast.condition
            binding.tempRangeTextView.text = "${forecast.minTemp}° / ${forecast.maxTemp}°"
            
            // 设置天气图标（这里简化处理，实际应该根据天气状况选择对应图标）
            binding.iconImageView.setImageResource(
                when (forecast.condition) {
                    "晴" -> android.R.drawable.ic_menu_day
                    "多云", "阴" -> android.R.drawable.ic_menu_gallery
                    "雨" -> android.R.drawable.ic_menu_call
                    "雪" -> android.R.drawable.ic_menu_camera
                    else -> android.R.drawable.ic_menu_compass
                }
            )
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val binding = ItemForecastBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ForecastViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        holder.bind(forecasts[position])
    }
    
    override fun getItemCount(): Int = forecasts.size
}