package com.example.asb.monitoring

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.asb.R
import com.example.asb.databinding.ActivityMonitoringBinding
import com.example.asb.mqtt.MqttCallbackHandler
import com.example.asb.mqtt.MqttClientManager
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.asb.models.DynamicEquipment
import com.example.asb.mqtt.MqttRepository
import com.example.asb.mqtt.MqttTestHelper
import com.example.asb.test.ElectricTestUtils
import com.example.asb.utils.JsonParser
import kotlinx.coroutines.launch

class MonitoringActivity : AppCompatActivity(), MqttCallbackHandler {
    private lateinit var binding: ActivityMonitoringBinding
    private lateinit var viewModel: MonitoringViewModel
    private lateinit var jsonParser: JsonParser
    private var ultimaPresion: Double? = null
    private lateinit var equipmentType: String
    private val currentMqttTopic: String by lazy {
        intent.getStringExtra("MQTT_TOPIC") ?: run {
            val clientId = intent.getStringExtra("CLIENT_ID") ?: "client_default"
            val projectId = intent.getStringExtra("PROJECT_ID") ?: "project_default"
            // Fallback seguro con registro de advertencia
            Log.w("MQTT", "Usando tópico fallback - Verificar MainActivity")
            "asb/telemetria/$clientId/$projectId/operaciones/bombas/data"
        }
    }
    private val useTestBroker = false

    // MQTT (Parte modificada)
    private lateinit var mqttManager: MqttClientManager // Única instancia

    private fun mostrarEquipos(equipos: List<DynamicEquipment>) {
        binding.equipmentContainer.removeAllViews()
        equipos.forEach { equipo ->
            mostrarEquipo(equipo.apply { tipo = equipmentType })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonitoringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        jsonParser = JsonParser()
        equipmentType = intent.getStringExtra("EQUIPMENT_TYPE") ?: "01" // ⬅️ Valor por defecto
        setupGauge()

        Log.d("MQTT", "Tópico utilizado: $currentMqttTopic")

        viewModel = ViewModelProvider(
            this,
            MonitoringViewModelFactory(
                MqttRepository(MqttClientManager.getInstance("ws://asbombeo.ddns.net:8083")),
                currentMqttTopic,
                equipmentType
            )

        )[MonitoringViewModel::class.java]
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MonitoringUiState.DataLoaded -> {
                            updateGauge(state.pressure ?: 2.5)
                            mostrarEquipos(state.equipmentList)
                        }
                        is MonitoringUiState.ConnectionError -> {
                            mostrarAlerta("MQTT", state.message ?: "Error")
                        }
                        MonitoringUiState.Connected -> {
                            binding.ivConnectionIcon.setImageResource(R.drawable.ic_cloud_done)
                        }
                        MonitoringUiState.Loading -> {
                            // Mostrar carga si es necesario
                        }
                    }
                }
            }
        }

        mqttManager = MqttClientManager.getInstance("ws://asbombeo.ddns.net:8083")
        mqttManager.addCallback(this)

        if (useTestBroker) {
            MqttTestHelper(this).startTestConnection()
        } else {
            mqttManager.connect { success ->
                if (success) mqttManager.subscribe(currentMqttTopic)
            }
        }
        binding.ivConnectionIcon.setImageResource(R.drawable.ic_cloud_sync)
        binding.tvConnectionStatus.text = getString(R.string.conectando)
        binding.connectionStatusContainer.setBackgroundColor(Color.parseColor("#FFEBEE"))
    }
    override fun onResume() {
        super.onResume()
        mqttManager.addCallback(this) // Siempre añade el callback (evita condiciones complejas)
        // Sincroniza el UI con el estado real:
        if (mqttManager.isConnected()) {
            onConnectionSuccess() // Actualiza el UI a "conectado"
        } else {
            onConnectionLost(Throwable("Desconectado")) // Actualiza el UI a "error"
        }
    }

    override fun onPause() {
        if (::mqttManager.isInitialized) {
            mqttManager.removeCallback(this as MqttCallbackHandler) // ✅ Correcto
        }
        super.onPause()
    }

    private fun setupGauge() {
        // Configuración inicial del gauge
        binding.pressureGauge.setPressure(2.5f)
        binding.tvPressureStatus.visibility = View.GONE
    }

    private fun updateGauge(pressure: Double) {
        binding.pressureGauge.setPressure(pressure.toFloat())
        binding.tvPressureStatus.text = when {
            pressure > 3.2 -> "ALTA PRESIÓN (${"%.2f".format(pressure)} kg/cm²)"
            pressure < 2.4 -> "BAJA PRESIÓN (${"%.2f".format(pressure)} kg/cm²)"
            else -> "PRESIÓN NORMAL (${"%.2f".format(pressure)} kg/cm²)"
        }
    }

    override fun onMessageReceived(topic: String, message: String) {
        runOnUiThread {
            val response = jsonParser.parseCombinedData(message) ?: return@runOnUiThread

            // 1. Mostrar presión en el gauge (SVV)
            if (equipmentType == "01") {
                binding.gaugeContainer.visibility = View.VISIBLE
                binding.tvPressureStatus.visibility = View.VISIBLE

                val pressure = response.presion ?: ultimaPresion ?: 2.5
                updateGauge(pressure)
                if (response.presion != null) ultimaPresion = pressure
            } else {
                binding.gaugeContainer.visibility = View.GONE
                binding.tvPressureStatus.visibility = View.GONE
            }

            // 2. Verificar anomalías
            response.equipos.forEach { equipo ->
                val estado = equipo.datos["ESTADO"]?.toString() ?: "DESCONOCIDO"

                // Verificar voltaje
                (equipo.datos["VOLTAJE_SALIDA"] as? Double)?.let { voltage ->
                    lifecycleScope.launch {
                        val esAnomalia = ElectricTestUtils.checkAndLogVoltage(
                            context = this@MonitoringActivity,
                            voltage = voltage,
                            equipmentName = equipo.nombre,
                            currentStatus = estado
                        )
                        if (esAnomalia) mostrarAlerta(equipo.nombre, "voltaje")
                    }
                }

                // Verificar presión (solo para SVV)
                if (equipmentType == "01") {
                    (equipo.datos["PRESION"] as? Double)?.let { pressure ->
                        lifecycleScope.launch {
                            val esAnomalia = ElectricTestUtils.checkAndLogPressure(
                                context = this@MonitoringActivity,
                                pressure = pressure,
                                equipmentName = equipo.nombre,
                                currentStatus = estado
                            )
                            if (esAnomalia) mostrarAlerta(equipo.nombre, "presión")
                        }
                    }
                }
            }

            // 3. Mostrar equipos
            binding.equipmentContainer.removeAllViews()
            response.equipos.forEach { equipo ->
                mostrarEquipo(equipo.apply { tipo = equipmentType })
            }
        }
    }

    private fun mostrarAlerta(nombreEquipo: String, tipoAnomalia: String) {
        Toast.makeText(
            this,
            "¡Alerta! $tipoAnomalia anormal en $nombreEquipo",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun mostrarEquipo(equipo: DynamicEquipment) {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_pozo_dynamic, binding.equipmentContainer, false)

        itemView.findViewById<ImageView>(R.id.ivEquipmentImage).setImageResource(
            when(equipo.tipo) {
                "01", "svv" -> R.mipmap.svv
                "02" -> R.mipmap.bomba_pozo
                "03" -> R.mipmap.hidro
                "04" -> R.mipmap.carcamo_2b
                else -> R.mipmap.asbombeo
            }
        )

        itemView.findViewById<TextView>(R.id.tvNombre).text = equipo.nombre

        val contenedor = itemView.findViewById<LinearLayout>(R.id.dynamicDataContainer)
        contenedor.removeAllViews()

        equipo.datos.forEach { (key, value) ->
            TextView(this).apply {
                text = getString(R.string.dynamic_data_format, key, value.toString())
                setTextColor(Color.BLACK)
                textSize = 16f
            }.also { contenedor.addView(it) }
        }

        binding.equipmentContainer.addView(itemView)
    }

    override fun onConnectionSuccess() {
        runOnUiThread {
            binding.ivConnectionIcon.setImageResource(R.drawable.ic_cloud_done)
            binding.tvConnectionStatus.text = getString(R.string.conectado)
            binding.connectionStatusContainer.setBackgroundColor(Color.parseColor("#E8F5E9")) // Verde claro
        }
    }

    override fun onConnectionLost(cause: Throwable) {
        runOnUiThread {
            binding.ivConnectionIcon.setImageResource(R.drawable.ic_cloud_off)
            binding.tvConnectionStatus.text = getString(R.string.desconectado)
            binding.connectionStatusContainer.setBackgroundColor(Color.parseColor("#FFEBEE")) // Rojo claro
        }
    }

    override fun onDestroy() {
        mqttManager.removeCallback(this as MqttCallbackHandler)
        if (useTestBroker) {
            MqttTestHelper(this).stopTestConnection()
        }
        super.onDestroy()
    }
}