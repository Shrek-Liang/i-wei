package com.example.weatherapp.data.cache

import android.content.Context
import android.content.SharedPreferences
import com.example.weatherapp.data.model.WeatherData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit

class WeatherCacheManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "weather_cache"
        private const val KEY_CACHED_WEATHER = "cached_weather_"
        private const val KEY_CACHE_TIME = "cache_time_"
        private const val KEY_FAVORITE_CITIES = "favorite_cities"
        private const val KEY_RECENT_CITIES = "recent_cities"
        private const val CACHE_DURATION_HOURS = 24L
    }
    
    /**
     * 缓存天气数据
     */
    fun cacheWeatherData(locationId: String, weatherData: WeatherData) {
        val json = gson.toJson(weatherData)
        prefs.edit()
            .putString(KEY_CACHED_WEATHER + locationId, json)
            .putLong(KEY_CACHE_TIME + locationId, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * 获取缓存的天气数据
     */
    fun getCachedWeatherData(locationId: String): WeatherData? {
        val json = prefs.getString(KEY_CACHED_WEATHER + locationId, null) ?: return null
        val cacheTime = prefs.getLong(KEY_CACHE_TIME + locationId, 0)
        
        // 检查缓存是否过期
        val currentTime = System.currentTimeMillis()
        val ageHours = TimeUnit.MILLISECONDS.toHours(currentTime - cacheTime)
        
        if (ageHours >= CACHE_DURATION_HOURS) {
            // 缓存已过期，清除
            clearCache(locationId)
            return null
        }
        
        return try {
            gson.fromJson(json, WeatherData::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 检查是否有缓存
     */
    fun hasCache(locationId: String): Boolean {
        return getCachedWeatherData(locationId) != null
    }
    
    /**
     * 清除指定城市的缓存
     */
    fun clearCache(locationId: String) {
        prefs.edit()
            .remove(KEY_CACHED_WEATHER + locationId)
            .remove(KEY_CACHE_TIME + locationId)
            .apply()
    }
    
    /**
     * 清除所有缓存
     */
    fun clearAllCache() {
        val keys = prefs.all.keys
        val editor = prefs.edit()
        keys.forEach { key ->
            if (key.startsWith(KEY_CACHED_WEATHER) || key.startsWith(KEY_CACHE_TIME)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }
    
    /**
     * 添加到常用城市列表
     */
    fun addFavoriteCity(locationId: String, cityName: String) {
        val favorites = getFavoriteCities().toMutableList()
        if (!favorites.any { it.locationId == locationId }) {
            favorites.add(FavoriteCity(locationId, cityName))
            prefs.edit()
                .putString(KEY_FAVORITE_CITIES, gson.toJson(favorites))
                .apply()
        }
    }
    
    /**
     * 从常用城市列表中移除
     */
    fun removeFavoriteCity(locationId: String) {
        val favorites = getFavoriteCities().toMutableList()
        favorites.removeAll { it.locationId == locationId }
        prefs.edit()
            .putString(KEY_FAVORITE_CITIES, gson.toJson(favorites))
            .apply()
    }
    
    /**
     * 获取常用城市列表
     */
    fun getFavoriteCities(): List<FavoriteCity> {
        val json = prefs.getString(KEY_FAVORITE_CITIES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<FavoriteCity>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 添加到最近查看的城市
     */
    fun addRecentCity(locationId: String, cityName: String) {
        val recent = getRecentCities().toMutableList()
        // 移除已存在的，然后添加到前面
        recent.removeAll { it.locationId == locationId }
        recent.add(0, FavoriteCity(locationId, cityName))
        // 只保留最近3个
        if (recent.size > 3) {
            recent.removeAt(recent.size - 1)
        }
        prefs.edit()
            .putString(KEY_RECENT_CITIES, gson.toJson(recent))
            .apply()
    }
    
    /**
     * 获取最近查看的城市
     */
    fun getRecentCities(): List<FavoriteCity> {
        val json = prefs.getString(KEY_RECENT_CITIES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<FavoriteCity>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 检查是否是常用城市
     */
    fun isFavoriteCity(locationId: String): Boolean {
        return getFavoriteCities().any { it.locationId == locationId }
    }
}

data class FavoriteCity(
    val locationId: String,
    val name: String
)