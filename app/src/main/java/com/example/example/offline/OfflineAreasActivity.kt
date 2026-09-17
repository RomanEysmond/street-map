package com.example.example.offline

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.example.databinding.ActivityOfflineAreasBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OfflineAreasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOfflineAreasBinding
    private val viewModel: OfflineAreasViewModel by viewModels()
    private val adapter = OfflineAreasAdapter(onDeleteClicked = { area -> viewModel.onDeleteClicked(area.id) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfflineAreasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.offlineAreasList.layoutManager = LinearLayoutManager(this)
        binding.offlineAreasList.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.areas.collect { areas ->
                    adapter.submitList(areas)
                    binding.emptyView.visibility = if (areas.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}
