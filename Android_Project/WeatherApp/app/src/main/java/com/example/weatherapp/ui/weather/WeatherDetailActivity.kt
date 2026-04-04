package com.example.weatherapp.ui.weather

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.R
import com.example.weatherapp.data.api.RetrofitClient
import com.example.weatherapp.data.cache.FavoriteCity
import com.example.weatherapp.data.cache.WeatherCacheManager
import com.example.weatherapp.data.model.City
import com.example.weatherapp.data.model.DailyForecast
import com.example.weatherapp.data.model.HourlyForecast
import com.example.weatherapp.data.model.WeatherData
import com.example.weatherapp.databinding.ActivityWeatherDetailBinding
import com.example.weatherapp.ui.search.CitySearchActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityWeatherDetailBinding
    private lateinit var cacheManager: WeatherCacheManager
    private var currentCity: City? = null
    private var isDarkMode = false
    private val forecastList = mutableListOf<DailyForecast>()
    private lateinit var forecastAdapter: ForecastAdapter
    private val favoriteCities = mutableListOf<FavoriteCity>()
    private lateinit var favoriteCityAdapter: FavoriteCityAdapter
    
    companion object {
        private const val REQUEST_CODE_SEARCH_CITY = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        cacheManager = WeatherCacheManager(this)
        
        // 检查深色模式设置
        isDarkMode = getDarkModePreference()
        updateDarkMode()
        
        setupToolbar()
        setupRecyclerViews()
        setupClickListeners()
        setupSwipeRefresh()
        
        // 检查是否有最近查看的城市
        val recentCities = cacheManager.getRecentCities()
        if (recentCities.isNotEmpty()) {
            loadWeatherData(recentCities[0].locationId, recentCities[0].name)
        } else {
            showEmptyState()
        }
        
        loadFavoriteCities()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerViews() {
        // 预报列表
        forecastAdapter = ForecastAdapter(forecastList)
        binding.forecastRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.forecastRecyclerView.adapter = forecastAdapter
        
        // 常用城市列表
        favoriteCityAdapter = FavoriteCityAdapter(favoriteCities) { city ->
            loadWeatherData(city.locationId, city.name)
        }
        binding.favoriteCitiesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.favoriteCitiesRecyclerView.adapter = favoriteCityAdapter
    }
    
    private fun setupClickListeners() {
        // 搜索城市
        binding.searchCard.setOnClickListener {
            val intent = Intent(this, CitySearchActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SEARCH_CITY)
        }
        
        // 切换常用城市
        binding.toggleFavoriteButton.setOnClickListener {
            currentCity?.let { city ->
                if (cacheManager.isFavoriteCity(city.locationId)) {
                    cacheManager.removeFavoriteCity(city.locationId)
                    binding.toggleFavoriteButton.text = "添加到常用城市"
                } else {
                    cacheManager.addFavoriteCity(city.locationId, city.name)
                    binding.toggleFavoriteButton.text = "从常用城市移除"
                }
                loadFavoriteCities()
            }
        }
        
        // 切换深色模式
        binding.darkModeButton.setOnClickListener {
            isDarkMode = !isDarkMode
            saveDarkModePreference(isDarkMode)
            updateDarkMode()
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            currentCity?.let { city ->
                loadWeatherData(city.locationId, city.name, forceRefresh = true)
            }
        }
    }
    
    private fun loadWeatherData(locationId: String, cityName: String, forceRefresh: Boolean = false) {
        lifecycleScope.launch {
            try {
                binding.swipeRefreshLayout.isRefreshing = true
                
                // 首先尝试从缓存加载
                if (!forceRefresh) {
                    val cachedData = cacheManager.getCachedWeatherData(locationId)
                    if (cachedData != null) {
                        displayWeatherData(cachedData)
                        binding.cacheHintTextView.visibility = View.VISIBLE
                    }
                }
                
                // 从网络获取最新数据
                val weatherData = withContext(Dispatchers.IO) {
                    fetchWeatherData(locationId, cityName)
                }
                
                if (weatherData != null) {
                    // 缓存数据
                    cacheManager.cacheWeatherData(locationId, weatherData)
                    
                    // 添加到最近查看的城市
                    cacheManager.addRecentCity(locationId, cityName)
                    
                    // 显示数据
                    displayWeatherData(weatherData)
                    binding.cacheHintTextView.visibility = View.GONE
                    
                    // 更新当前城市
                    currentCity = weatherData.city
                    updateFavoriteButton()
                } else if (!forceRefresh) {
                    // 如果网络请求失败且不是强制刷新，使用缓存数据
                    val cachedData = cacheManager.getCachedWeatherData(locationId)
                    if (cachedData != null) {
                        displayWeatherData(cachedData)
                        binding.cacheHintTextView.visibility = View.VISIBLE
                        binding.cacheHintTextView.text = "网络错误，展示缓存数据"
                    }
                }
            } catch (e: Exception) {
                // 网络错误，尝试使用缓存
                val cachedData = cacheManager.getCachedWeatherData(locationId)
                if (cachedData != null) {
                    displayWeatherData(cachedData)
                    binding.cacheHintTextView.visibility = View.VISIBLE
                    binding.cacheHintTextView.text = "网络错误，展示缓存数据"
                }
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private suspend fun fetchWeatherData(locationId: String, cityName: String): WeatherData? {
        return withContext(Dispatchers.IO) {
            try {
                // 调用聚合数据API
                val response = RetrofitClient.apiService.getWeather(cityName, RetrofitClient.getApiKey()).execute()
                
                if (response.isSuccessful && response.body() != null) {
                    val juheResponse = response.body()!!
                    
                    if (juheResponse.error_code == 0 && juheResponse.result != null) {
                        val result = juheResponse.result!!
                        val realtime = result.realtime
                        val future = result.future ?: emptyList()
                        
                        val city = City(cityName, cityName, "", "")
                        
                        val currentWeather = com.example.weatherapp.data.model.CurrentWeather(
                            temperature = realtime?.temperature ?: "--",
                            feelsLike = realtime?.temperature ?: "--",
                            condition = realtime?.info ?: "未知",
                            icon = realtime?.wid ?: "",
                            windDirection = realtime?.direct ?: "--",
                            windScale = realtime?.power ?: "--",
                            humidity = realtime?.humidity ?: "--"
                        )
                        
                        val dailyForecast = future.map { day ->
                            // 解析温度范围，格式如 "15/25"
                            val temps = day.temperature.split("/")
                            val minTemp = if (temps.size >= 2) temps[1] else "--"
                            val maxTemp = if (temps.size >= 1) temps[0] else "--"
                            
                            DailyForecast(
                                date = day.date,
                                maxTemp = maxTemp,
                                minTemp = minTemp,
                                condition = day.weather,
                                icon = day.wid?.day ?: ""
                            )
                        }
                        
                        WeatherData(
                            city = city,
                            currentWeather = currentWeather,
                            dailyForecast = dailyForecast,
                            hourlyForecast = emptyList(),
                            cached = false
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private fun displayWeatherData(weatherData: WeatherData) {
        currentCity = weatherData.city
        
        // 更新UI
        binding.cityNameTextView.text = weatherData.city.name
        binding.temperatureTextView.text = "${weatherData.currentWeather.temperature}°"
        binding.conditionTextView.text = weatherData.currentWeather.condition
        binding.feelsLikeTextView.text = "体感温度: ${weatherData.currentWeather.feelsLike}°"
        binding.humidityTextView.text = "湿度: ${weatherData.currentWeather.humidity}%"
        binding.windTextView.text = "风力: ${weatherData.currentWeather.windDirection} ${weatherData.currentWeather.windScale}级"
        
        // 更新时间
        val updateTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        binding.updateTimeTextView.text = "更新时间: $updateTime"
        
        // 更新预报列表
        forecastList.clear()
        forecastList.addAll(weatherData.dailyForecast)
        forecastAdapter.notifyDataSetChanged()
        
        // 更新常用城市按钮状态
        updateFavoriteButton()
        
        binding.currentWeatherCard.visibility = View.VISIBLE
    }
    
    private fun loadFavoriteCities() {
        favoriteCities.clear()
        favoriteCities.addAll(cacheManager.getFavoriteCities())
        favoriteCityAdapter.notifyDataSetChanged()
        
        if (favoriteCities.isEmpty()) {
            binding.favoriteCitiesRecyclerView.visibility = View.GONE
        } else {
            binding.favoriteCitiesRecyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun updateFavoriteButton() {
        currentCity?.let { city ->
            if (cacheManager.isFavoriteCity(city.locationId)) {
                binding.toggleFavoriteButton.text = "从常用城市移除"
            } else {
                binding.toggleFavoriteButton.text = "添加到常用城市"
            }
        }
    }
    
    private fun showEmptyState() {
        binding.cityNameTextView.text = "请搜索城市"
        binding.temperatureTextView.text = "--°"
        binding.conditionTextView.text = ""
        binding.feelsLikeTextView.text = "体感温度: --°"
        binding.humidityTextView.text = "湿度: --%"
        binding.windTextView.text = "风力: --"
        binding.cacheHintTextView.visibility = View.GONE
    }
    
    private fun getDarkModePreference(): Boolean {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("dark_mode", false)
    }
    
    private fun saveDarkModePreference(isDark: Boolean) {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("dark_mode", isDark).apply()
    }
    
    private fun updateDarkMode() {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            binding.darkModeButton.text = "切换浅色模式"
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            binding.darkModeButton.text = "切换深色模式"
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SEARCH_CITY && resultCode == RESULT_OK) {
            data?.let {
                val cityName = it.getStringExtra("cityName") ?: ""
                val locationId = it.getStringExtra("locationId") ?: ""
                if (cityName.isNotEmpty() && locationId.isNotEmpty()) {
                    loadWeatherData(locationId, cityName, forceRefresh = true)
                }
            }
        }
    }
}