package com.example.weatherapp.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.R
import com.example.weatherapp.data.api.LocationItem
import com.example.weatherapp.databinding.ActivityCitySearchBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CitySearchActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCitySearchBinding
    private val citiesList = mutableListOf<LocationItem>()
    private lateinit var adapter: CitySearchAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupSearch()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = CitySearchAdapter(citiesList) { city ->
            // 返回选中的城市信息
            intent.putExtra("cityName", city.name)
            intent.putExtra("locationId", city.locationId)
            intent.putExtra("lat", city.lat)
            intent.putExtra("lon", city.lon)
            setResult(RESULT_OK, intent)
            finish()
        }
        
        binding.citiesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.citiesRecyclerView.adapter = adapter
    }
    
    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().isNotEmpty()) {
                    searchCities(s.toString())
                } else {
                    showEmptyState()
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchEditText.text.toString()
                if (query.isNotEmpty()) {
                    searchCities(query)
                }
                true
            } else {
                false
            }
        }
    }
    
    private fun searchCities(query: String) {
        lifecycleScope.launch {
            try {
                showLoading()
                
                // 聚合数据API不支持城市搜索，直接使用输入的城市名
                citiesList.clear()
                citiesList.add(LocationItem(query, query, "", ""))
                
                showResults()
            } catch (e: Exception) {
                showEmptyState("搜索失败，请稍后重试")
            }
        }
    }
    
    private fun showLoading() {
        binding.citiesRecyclerView.visibility = View.GONE
        binding.emptyTextView.text = "搜索中..."
        binding.emptyTextView.visibility = View.VISIBLE
    }
    
    private fun showResults() {
        binding.emptyTextView.visibility = View.GONE
        binding.citiesRecyclerView.visibility = View.VISIBLE
        adapter.notifyDataSetChanged()
    }
    
    private fun showEmptyState(message: String = "请输入城市名称进行搜索") {
        binding.emptyTextView.text = message
        binding.emptyTextView.visibility = View.VISIBLE
        binding.citiesRecyclerView.visibility = View.GONE
    }
}