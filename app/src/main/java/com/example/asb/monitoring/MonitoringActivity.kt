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
import com.example.asb.models.DynamicEquipment
import com.example.asb.utils.JsonParser

class MonitoringActivity : AppCompatActivity(), MqttCallbackHandler {
    private lateinit var binding: ActivityMonitoringBinding
    private lateinit var mqttManager: MqttClientManager
    private lateinit var mqttTopic: String
    private lateinit var equipmentType: String
    private lateinit var jsonParser: JsonParser
    private var ultimaPresion: Double? = null

    private fun setupGauge() {
        // Configuración inicial
        binding.pressureGauge.setPressure(2.5f)
        binding.tvPressureStatus.visibility = View.GONE
    }

    private fun updateGauge(pressure: Double) {
        binding.pressureGauge.setPressure(pressure.toFloat())

        // Actualizar estado textual
        binding.tvPressureStatus.text = when {
            pressure > 3.2 -> "ALTA PRESIÓN (${"%.2f".format(pressure)} kg/cm²)"
            pressure < 2.4 -> "BAJA PRESIÓN (${"%.2f".format(pressure)} kg/cm²)"
            else -> "PRESIÓN NORMAL (${"%.2f".format(pressure)} kg/cm²)"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonitoringBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupGauge()

        jsonParser = JsonParser()

        // 1. Obtener parámetros
        mqttTopic = intent.getStringExtra("MQTT_TOPIC_DATA") ?: "001/0001/02/02/Datos"
        equipmentType = intent.getStringExtra("EQUIPMENT_TYPE") ?: "02"
        Log.d("EQUIPMENT_DEBUG", "Tipo de equipo recibido: $equipmentType")  // ← Añadir esto


        // 2. Configurar MQTT
        mqttManager = MqttClientManager("tcp://test.mosquitto.org:1883").apply {
            setCallback(this@MonitoringActivity)
            connect { success ->
                if (success) subscribe(mqttTopic, 1)
            }
        }
    }

    override fun onMessageReceived(topic: String, message: String) {
        runOnUiThread {
            val response = jsonParser.parseCombinedData(message) ?: return@runOnUiThread

            if (equipmentType == "01") { // Solo para SVV
                binding.gaugeContainer.visibility = View.VISIBLE
                binding.tvPressureStatus.visibility = View.VISIBLE

                val pressure = response.presion ?: ultimaPresion ?: 2.5
                updateGauge(pressure)

                if (response.presion != null) ultimaPresion = pressure
            } else {
                binding.gaugeContainer.visibility = View.GONE
                binding.tvPressureStatus.visibility = View.GONE
            }

            // Resto de tu lógica para mostrar equipos...
            binding.equipmentContainer.removeAllViews()
            response.equipos.forEach { equipo ->
                mostrarEquipo(equipo.apply { tipo = equipmentType })
            }
        }
    }
    private fun mostrarEquipo(equipo: DynamicEquipment) {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_pozo_dynamic, binding.equipmentContainer, false)

        // Imagen según el tipo (usando equipo.tipo)
        itemView.findViewById<ImageView>(R.id.ivEquipmentImage).setImageResource(
            when(equipo.tipo) {
                "01" -> R.mipmap.svv
                "02" -> R.mipmap.bomba_pozo
                "03" -> R.mipmap.hidro
                "04" -> R.mipmap.carcamo_2b
                else -> R.mipmap.asbombeo
            }
        )

        // Nombre del equipo
        itemView.findViewById<TextView>(R.id.tvNombre).text = equipo.nombre

        // Mostrar datos dinámicos
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
            binding.tvConnectionStatus.text = getString(R.string.connected_mqtt)
            binding.tvConnectionStatus.setBackgroundColor(Color.GREEN)
        }
    }

    override fun onConnectionLost(cause: Throwable) {
        runOnUiThread {
            binding.tvConnectionStatus.text = getString(R.string.connection_lost, cause.message ?: "Sin mensaje")
            binding.tvConnectionStatus.setBackgroundColor(Color.RED)
        }
    }

    override fun onDestroy() {
        mqttManager.disconnect()
        super.onDestroy()
    }
}