package com.example.weatherapp.ui.weather

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.data.cache.FavoriteCity
import com.example.weatherapp.databinding.ItemFavoriteCityBinding

class FavoriteCityAdapter(
    private val cities: List<FavoriteCity>,
    private val onItemClick: (FavoriteCity) -> Unit
) : RecyclerView.Adapter<FavoriteCityAdapter.FavoriteCityViewHolder>() {
    
    inner class FavoriteCityViewHolder(private val binding: ItemFavoriteCityBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(city: FavoriteCity) {
            binding.cityNameTextView.text = city.name
            binding.temperatureTextView.text = "--°" // 这里应该从缓存中获取温度
            
            binding.root.setOnClickListener {
                onItemClick(city)
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteCityViewHolder {
        val binding = ItemFavoriteCityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavoriteCityViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: FavoriteCityViewHolder, position: Int) {
        holder.bind(cities[position])
    }
    
    override fun getItemCount(): Int = cities.size
}