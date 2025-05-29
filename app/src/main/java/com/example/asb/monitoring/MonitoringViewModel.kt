package com.example.asb.monitoring

import androidx.lifecycle.ViewModel
import com.example.asb.models.DynamicEquipment
import com.example.asb.mqtt.MqttCallbackHandler
import com.example.asb.mqtt.MqttRepository
import com.example.asb.utils.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MonitoringViewModel(
    mqttRepository: MqttRepository,
    topic: String,
    private val equipmentType: String
) : ViewModel() {

    private val mqttCallback = object : MqttCallbackHandler {
        override fun onMessageReceived(topic: String, message: String) {
            val response = jsonParser.parseCombinedData(message) ?: return
            response.presion?.let { ultimaPresion = it }
            _uiState.value = MonitoringUiState.DataLoaded(
                pressure = response.presion ?: ultimaPresion,
                equipmentList = response.equipos,
                equipmentType = equipmentType
            )
        }

        override fun onConnectionLost(cause: Throwable) {
            _uiState.value = MonitoringUiState.ConnectionError(cause.message)
        }

        override fun onConnectionSuccess() {
            _uiState.value = MonitoringUiState.Connected
        }
    }

    // Variables originales de tu Activity
    private var ultimaPresion: Double? = null
    private val jsonParser = JsonParser() // Mantenemos tu parser

    // Flujos para comunicación con la Activity
    private val _uiState = MutableStateFlow<MonitoringUiState>(MonitoringUiState.Loading)
    val uiState: StateFlow<MonitoringUiState> = _uiState

    init {
        mqttRepository.subscribe(topic, mqttCallback)
    }

}

// Estados posibles de la UI (sin lógica de test)
sealed class MonitoringUiState {
    data object Loading : MonitoringUiState() // ✅
    data object Connected : MonitoringUiState() // ✅
    data class ConnectionError(val message: String?) : MonitoringUiState()
    data class DataLoaded(
        val pressure: Double?,
        val equipmentList: List<DynamicEquipment>,
        val equipmentType: String
    ) : MonitoringUiState()
}