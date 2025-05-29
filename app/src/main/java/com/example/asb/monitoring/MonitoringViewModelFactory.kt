package com.example.asb.monitoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.asb.mqtt.MqttRepository

class MonitoringViewModelFactory(
    private val repository: MqttRepository,
    private val topic: String,
    private val equipmentType: String // Pasamos tu variable
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MonitoringViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MonitoringViewModel(repository, topic, equipmentType) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}