package com.example.weatherapp.data.model

// 聚合数据API响应模型
data class JuHeWeatherResponse(
    val error_code: Int = 0,
    val reason: String = "",
    val result: JuHeWeatherResult? = null
)

data class JuHeWeatherResult(
    val realtime: JuHeRealtime? = null,
    val future: List<JuHeFuture>? = null
)

data class JuHeRealtime(
    val temperature: String = "",
    val humidity: String = "",
    val info: String = "",
    val wid: String = "",
    val direct: String = "",
    val power: String = "",
    val aqi: String = ""
)

data class JuHeFuture(
    val date: String = "",
    val temperature: String = "",
    val weather: String = "",
    val wid: Wid? = null,
    val direct: String = ""
)

data class Wid(
    val day: String = "",
    val night: String = ""
)

// 应用内部使用的数据模型
data class WeatherData(
    val city: City,
    val currentWeather: CurrentWeather,
    val dailyForecast: List<DailyForecast>,
    val hourlyForecast: List<HourlyForecast>,
    val cached: Boolean = false
)

data class City(
    val name: String = "",
    val locationId: String = "",
    val lat: String = "",
    val lon: String = ""
)

data class CurrentWeather(
    val temperature: String,
    val feelsLike: String,
    val condition: String,
    val icon: String,
    val windDirection: String,
    val windScale: String,
    val humidity: String
)

data class DailyForecast(
    val date: String,
    val maxTemp: String,
    val minTemp: String,
    val condition: String,
    val icon: String
)

data class HourlyForecast(
    val time: String,
    val temperature: String,
    val condition: String,
    val icon: String
)