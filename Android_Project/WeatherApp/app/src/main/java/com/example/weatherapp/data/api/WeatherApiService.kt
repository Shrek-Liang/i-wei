package com.example.weatherapp.data.api

import com.example.weatherapp.data.model.JuHeWeatherResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("simpleWeather/query")
    fun getWeather(
        @Query("city") city: String,
        @Query("key") key: String
    ): Call<JuHeWeatherResponse>
}

data class LocationItem(
    val name: String = "",
    val locationId: String = "",
    val lat: String = "",
    val lon: String = ""
)

object RetrofitClient {
    private const val BASE_URL = "https://apis.juhe.cn/"
    private const val API_KEY = "d9457f643efb6f22f23c650ad7f0f7d5" // 聚合数据API密钥

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: WeatherApiService by lazy {
        retrofit.create(WeatherApiService::class.java)
    }

    fun getApiKey(): String = API_KEY

    fun updateApiKey(newKey: String) {
        // 可以在这里实现动态更新API密钥的逻辑
    }
}