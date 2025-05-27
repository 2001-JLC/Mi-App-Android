package com.example.asb.test

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.asb.databinding.ActivityEquipmentHistoryBinding

class EquipmentHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEquipmentHistoryBinding
    private val viewModel: AnomaliesViewModel by viewModels()
    private lateinit var equipmentName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEquipmentHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        equipmentName = intent.getStringExtra("EQUIPMENT_NAME") ?: return finish()

        supportActionBar?.title = "Historial de $equipmentName"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        observeAnomalies()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = AnomaliesAdapter()
    }

    private fun observeAnomalies() {
        viewModel.getAnomaliesForEquipment(equipmentName).observe(this) { anomalies ->
            (binding.recyclerView.adapter as AnomaliesAdapter).submitList(anomalies)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}