package com.example.asb.db

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.asb.R
import com.example.asb.db.json.ElectricDataAdapter
import com.example.asb.db.json.ElectricDataResponse
import com.example.asb.mqtt.MqttCallbackHandler
import com.example.asb.mqtt.MqttProductionHelper
import com.example.asb.topic.MqttTopicManager
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Locale

class DataActivity : AppCompatActivity(), MqttCallbackHandler {

    // Variables MQTT
    private lateinit var mqttHelper: MqttProductionHelper
    private lateinit var requestTopic: String
    private lateinit var responseTopic: String

    // Variables UI
    private lateinit var adapter: ElectricDataAdapter
    private lateinit var rvElectricData: RecyclerView
    private lateinit var tvLastUpdate: TextView
    private lateinit var tvVoltage: TextView
    private lateinit var tvCurrent: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar

    // Utils
    private val gson = Gson()
    private val dateFormatDisplay = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val dateFormatParser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)

        initViews()
        setupRecyclerView()
        initMqtt()
    }

    private fun initViews() {
        rvElectricData = findViewById(R.id.rv_electric_data)
        tvLastUpdate = findViewById(R.id.tv_last_update)
        tvVoltage = findViewById(R.id.tv_voltage)
        tvCurrent = findViewById(R.id.tv_current)
        tvStatus = findViewById(R.id.tv_status)
        progressBar = findViewById(R.id.progress_bar)

        // Mostrar progreso inicial
        progressBar.visibility = View.VISIBLE
        tvStatus.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        adapter = ElectricDataAdapter()
        rvElectricData.layoutManager = LinearLayoutManager(this)
        rvElectricData.adapter = adapter
    }

    private fun initMqtt() {
        val clientId = intent.getStringExtra("CLIENT_ID") ?: "client_default"
        val projectId = intent.getStringExtra("WORK_ORDER") ?: "project_default"

        requestTopic = MqttTopicManager.getDataRequestTopic(clientId, projectId)
        responseTopic = MqttTopicManager.getDataResponseTopic(clientId, projectId)

        mqttHelper = MqttProductionHelper(this).apply { connect() }
    }

    override fun onConnectionSuccess() {
        Log.d("DataActivity", "Conectado a MQTT. Suscribiendo a $responseTopic")
        mqttHelper.subscribe(responseTopic)
        requestData()
    }

    override fun onConnectionLost(cause: Throwable) {
        runOnUiThread {
            progressBar.visibility = View.GONE
            tvStatus.text = getString(R.string.error_connection, cause.message ?: "Desconocido")
            tvStatus.visibility = View.VISIBLE
        }
        Log.e("DataActivity", "Conexión perdida", cause)
    }

    override fun onMessageReceived(topic: String, message: String) {
        if (topic == responseTopic) {
            Log.d("DataActivity", "Datos recibidos: $message")
            processReceivedData(message)
        }
    }

    private fun processReceivedData(jsonData: String) {
        try {
            val data = gson.fromJson(jsonData, ElectricDataResponse::class.java)
            updateUI(data)
        } catch (e: Exception) {
            showError("Error al procesar datos")
            Log.e("DataActivity", "Error parsing JSON", e)
        }
    }

    private fun updateUI(data: ElectricDataResponse) {
        runOnUiThread {
            try {
                progressBar.visibility = View.GONE

                if (data.equipo1.registros.isNotEmpty()) {
                    val firstRecord = data.equipo1.registros[0]
                    val parsedDate = dateFormatParser.parse(firstRecord.timestamp)

                    // Corregido: Manejo seguro de fechas
                    parsedDate?.let {
                        tvLastUpdate.text = dateFormatDisplay.format(it)
                    } ?: run {
                        tvLastUpdate.text = "--/--/---- --:--"
                    }
                    tvVoltage.text = getString(R.string.voltage_format, firstRecord.voltaje)
                    tvCurrent.text = getString(R.string.current_format, firstRecord.corriente)

                    // Actualizar lista
                    adapter.submitList(data.equipo1.registros)
                } else {
                    showError("No hay datos disponibles")
                }
            } catch (e: Exception) {
                showError("Error mostrando datos")
                Log.e("DataActivity", "UI Error", e)
            }
        }
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        tvStatus.text = message
        tvStatus.visibility = View.VISIBLE
    }

    private fun requestData() {
        mqttHelper.publish(requestTopic, "get_data")
        Log.d("DataActivity", "Solicitando datos...")
    }

    override fun onDestroy() {
        mqttHelper.unsubscribe(responseTopic)
        mqttHelper.disconnect()
        super.onDestroy()
    }
}