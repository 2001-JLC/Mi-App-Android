package com.example.asb.db

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.asb.databinding.ActivityDataBinding
import com.example.asb.test.AnomaliesViewModel
import com.example.asb.test.EquipmentAdapter
import com.example.asb.test.EquipmentHistoryActivity

class DataActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDataBinding
    private val viewModel: AnomaliesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Seleccionar Equipo"
        setupRecyclerView()
        observeEquipmentList()
    }

    private fun setupRecyclerView() {
        binding.equipmentRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.equipmentRecyclerView.adapter = EquipmentAdapter { equipmentName ->
            startActivity(Intent(this, EquipmentHistoryActivity::class.java).apply {
                putExtra("EQUIPMENT_NAME", equipmentName)
            })
        }
    }

    private fun observeEquipmentList() {
        viewModel.equipmentNames.observe(this) { equipmentNames ->
            (binding.equipmentRecyclerView.adapter as EquipmentAdapter).submitList(equipmentNames)
        }
    }
}