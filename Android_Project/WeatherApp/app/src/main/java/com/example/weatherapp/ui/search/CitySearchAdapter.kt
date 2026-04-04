package com.example.weatherapp.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.data.api.LocationItem
import com.example.weatherapp.databinding.ItemCitySearchBinding

class CitySearchAdapter(
    private val cities: List<LocationItem>,
    private val onItemClick: (LocationItem) -> Unit
) : RecyclerView.Adapter<CitySearchAdapter.CityViewHolder>() {
    
    inner class CityViewHolder(private val binding: ItemCitySearchBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(city: LocationItem) {
            binding.cityNameTextView.text = city.name
            binding.locationInfoTextView.text = "${city.lat}, ${city.lon}"
            
            binding.root.setOnClickListener {
                onItemClick(city)
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val binding = ItemCitySearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CityViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        holder.bind(cities[position])
    }
    
    override fun getItemCount(): Int = cities.size
}