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
import com.example.asb.models.DynamicEquipment
import com.example.asb.mqtt.MqttTestHelper
import com.example.asb.utils.JsonParser

class MonitoringActivity : AppCompatActivity(), MqttCallbackHandler {
    private lateinit var binding: ActivityMonitoringBinding
    private lateinit var mqttManager: MqttClientManager
    private lateinit var mqttTestHelper: MqttTestHelper //Quitar o comentar despues de la prueba
    // Eliminamos la declaración redundante de mqttTopic aquí
    private lateinit var equipmentType: String
    private lateinit var jsonParser: JsonParser
    private var ultimaPresion: Double? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonitoringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Estado: Conectando (al iniciar la actividad)
        binding.ivConnectionIcon.setImageResource(R.drawable.ic_cloud_sync)
        binding.tvConnectionStatus.text = getString(R.string.conectando)
        binding.connectionStatusContainer.setBackgroundColor(Color.parseColor("#FFEBEE")) // Rojo claro


        // Inicializamos el jsonParser
        jsonParser = JsonParser()

        // Obtenemos el tipo de equipo del intent
        //equipmentType = intent.getStringExtra("EQUIPMENT_TYPE") ?: "02" // Valor por defecto
        equipmentType = "01"

        // Llamamos a setupGauge al inicio
        setupGauge()

        // 1. Obtén los datos de la OT seleccionada
        val clientId = intent.getStringExtra("CLIENT_ID") ?: "client_default"
        val projectId = intent.getStringExtra("PROJECT_ID") ?: "project_default"

        // 2. Genera el tópico dinámico (usamos variable local, no propiedad de clase)
        val mqttTopic = "asb/telemetria/$clientId/$projectId/operaciones/bombas/data"
        Log.d("MQTT_TOPIC", "Tópico generado: $mqttTopic")

        // 3. Conecta al broker con el tópico
        val brokerUrl = "ws://tu_broker:8083/mqtt"  // Ajusta la URL
        mqttManager = MqttClientManager(brokerUrl)
        mqttManager.setCallback(this)
        mqttManager.connect { success ->
            if (success) {
                // onConnectionSuccess() se llamará automáticamente vía callback
                mqttManager.subscribe(mqttTopic)
            } else {
                runOnUiThread {
                    // Actualiza la UI si falla inmediatamente
                    binding.ivConnectionIcon.setImageResource(R.drawable.ic_cloud_off)
                    binding.tvConnectionStatus.text = getString(R.string.desconectado)
                    binding.connectionStatusContainer.setBackgroundColor(Color.parseColor("#FFEBEE"))
                    Toast.makeText(this, "Error al conectar", Toast.LENGTH_SHORT).show()
                }
            }
        }
        // Conexión de PRUEBA (Mosquitto) - Temporal
        mqttTestHelper = MqttTestHelper(this) //quitar o comentar despues de las pruebas
        mqttTestHelper.startTestConnection() //quitar o comentar

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

            binding.equipmentContainer.removeAllViews()
            response.equipos.forEach { equipo ->
                mostrarEquipo(equipo.apply { tipo = equipmentType })
            }
        }
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

    // En tu MonitoringActivity:
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
        mqttTestHelper.stopTestConnection() //comentar o quitar(es para la prueba cons moquito)
        mqttManager.disconnect()
        super.onDestroy()
    }
}