package com.example.asb.monitoring

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.asb.R
import com.example.asb.databinding.ActivityMonitoringBinding
import com.example.asb.mqtt.MqttCallbackHandler
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.example.asb.models.DynamicEquipment
import com.example.asb.mqtt.AppConfig
import com.example.asb.mqtt.MqttProductionHelper
import com.example.asb.mqtt.MqttTestHelper
import com.example.asb.topic.MqttTopicManager
import com.example.asb.utils.JsonParser


class MonitoringActivity : AppCompatActivity(), MqttCallbackHandler {
    private lateinit var binding: ActivityMonitoringBinding
    private lateinit var mqttHelper: MqttTestHelper
    private lateinit var jsonParser: JsonParser
    private var ultimaPresion: Double? = null
    private lateinit var equipmentType: String
    private lateinit var mqttTopic: String
    private lateinit var mqttProductionHelper: MqttProductionHelper

    private fun setupGauge() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonitoringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        jsonParser = JsonParser()
        equipmentType = intent.getStringExtra("EQUIPMENT_TYPE") ?: "01"

        mqttTopic = intent.getStringExtra("MQTT_TOPIC")
            ?: MqttTopicManager.getMonitoringTopic(  // Fallback si no hay tópico previo
                intent.getStringExtra("CLIENT_ID") ?: "client_default",
                intent.getStringExtra("WORK_ORDER") ?: "project_default"
            )
        Log.d("MQTT_DEBUG", "Tópico generado: $mqttTopic")
        Log.d("MQTT_DEBUG", "Modo TEST: ${AppConfig.isTestMode}")

        equipmentType = mqttTopic.split("/").getOrNull(2) ?: "01"
        setupGauge()

        // Estado inicial de conexión
        binding.ivConnectionIcon.setImageResource(R.drawable.ic_cloud_sync)
        binding.tvConnectionStatus.text = getString(R.string.conectando)
        binding.connectionStatusContainer.setBackgroundColor(Color.parseColor("#FFEBEE"))

        if (AppConfig.isTestMode) {
            mqttHelper = MqttTestHelper(this)
            Log.d("MQTT_DEBUG", "Iniciando conexión TEST a broker...")
            mqttHelper.connect()
        } else {
            mqttProductionHelper = MqttProductionHelper(this)
            Log.d("MQTT_DEBUG", "Iniciando conexión PRODUCCIÓN a broker...")
            mqttProductionHelper.connect()
        }
    }

    override fun onMessageReceived(topic: String, message: String) {
        runOnUiThread {
            val response = jsonParser.parseCombinedData(message) ?: return@runOnUiThread

            // Mostrar u ocultar el medidor basado en si hay datos de presión
            if (response.presion != null) {  // ← Cambio clave aquí
                binding.gaugeContainer.visibility = View.VISIBLE
                binding.tvPressureStatus.visibility = View.VISIBLE
                updateGauge(response.presion)  // Usar el valor directamente
                ultimaPresion = response.presion  // Actualizar último valor
            } else {
                binding.gaugeContainer.visibility = View.GONE
                binding.tvPressureStatus.visibility = View.GONE
            }

            // Mostrar equipos (sin cambios)
            binding.equipmentContainer.removeAllViews()
            response.equipos.forEach { equipo ->
                mostrarEquipo(equipo)
            }
        }
    }

    private fun mostrarEquipo(equipo: DynamicEquipment) {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_pozo_dynamic, binding.equipmentContainer, false)
        // Asignar imagen basada en el tipo (ahora con valores descriptivos)
        itemView.findViewById<ImageView>(R.id.ivEquipmentImage).setImageResource(
            when (equipo.tipo.uppercase()) {  // ← Usamos uppercase() para evitar case-sensitive
                "SVV" -> R.mipmap.svv
                "POZO" -> R.mipmap.bomba_pozo      // Ejemplo para futuro
                "HIDRO" -> R.mipmap.hidro          // Ejemplo para futuro
                "CARCAMO" -> R.mipmap.carcamo_2b   // Ejemplo para futuro
                else -> R.mipmap.asbombeo          // Imagen por defecto
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
            Log.d("MQTT_DEBUG", "🔄🔄🔄 onConnectionSuccess() llamado")
            binding.ivConnectionIcon.setImageResource(R.drawable.ic_cloud_done)
            binding.tvConnectionStatus.text = getString(R.string.conectado)
            binding.connectionStatusContainer.setBackgroundColor(Color.parseColor("#E8F5E9")) // Verde claro
            // ¡Agrega esto! (Suscribirse cuando la conexión esté lista)
            if (AppConfig.isTestMode) {
                mqttHelper.subscribe(mqttTopic) // <-- Suscripción al tópico dinámico
            } else {
                mqttProductionHelper.subscribe(mqttTopic)
            }
        }
    }

    override fun onConnectionLost(cause: Throwable) {
        runOnUiThread {
            binding.ivConnectionIcon.setImageResource(R.drawable.ic_cloud_off)
            binding.tvConnectionStatus.text = getString(R.string.desconectado)
            binding.connectionStatusContainer.setBackgroundColor(Color.parseColor("#FFEBEE")) // Rojo claro
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("MQTT_DEBUG", "onStop() - Desuscribiendo y desconectando")

        if (AppConfig.isTestMode) {
            mqttHelper.unsubscribe(mqttTopic)  // Desuscribir del tópico
            mqttHelper.disconnect()            // Opcional: desconectar completamente
        } else {
            mqttProductionHelper.unsubscribe(mqttTopic)
            mqttProductionHelper.disconnect()
        }
    }

    override fun onDestroy() {
        // Limpieza adicional por si onStop() no se ejecutó
        if (AppConfig.isTestMode) {
            mqttHelper.unsubscribe(mqttTopic)
            mqttHelper.disconnect()
        } else {
            mqttProductionHelper.unsubscribe(mqttTopic)
            mqttProductionHelper.disconnect()
        }
        super.onDestroy()
    }
}