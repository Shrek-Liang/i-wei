package com.example.weatherapp.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.weatherapp.R
import com.example.weatherapp.data.cache.WeatherCacheManager
import com.example.weatherapp.ui.weather.WeatherDetailActivity

class WeatherWidgetProvider : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 更新所有小组件
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onEnabled(context: Context) {
        // 当第一个小组件被添加时调用
    }
    
    override fun onDisabled(context: Context) {
        // 当最后一个小组件被移除时调用
    }
    
    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val cacheManager = WeatherCacheManager(context)
            val recentCities = cacheManager.getRecentCities()
            
            val views = RemoteViews(context.packageName, R.layout.widget_weather)
            
            if (recentCities.isNotEmpty()) {
                val city = recentCities[0]
                val weatherData = cacheManager.getCachedWeatherData(city.locationId)
                
                if (weatherData != null) {
                    views.setTextViewText(R.id.widgetCityName, city.name)
                    views.setTextViewText(
                        R.id.widgetTemperature,
                        "${weatherData.currentWeather.temperature}°"
                    )
                    views.setTextViewText(
                        R.id.widgetCondition,
                        weatherData.currentWeather.condition
                    )
                } else {
                    views.setTextViewText(R.id.widgetCityName, city.name)
                    views.setTextViewText(R.id.widgetTemperature, "--°")
                    views.setTextViewText(R.id.widgetCondition, "暂无数据")
                }
            } else {
                views.setTextViewText(R.id.widgetCityName, "天气")
                views.setTextViewText(R.id.widgetTemperature, "--°")
                views.setTextViewText(R.id.widgetCondition, "请搜索城市")
            }
            
            // 点击小组件打开应用
            val intent = Intent(context, WeatherDetailActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            views.setOnClickPendingIntent(
                R.id.widgetLayout,
                android.app.PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}