package com.example.asb.faults

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.asb.R
import com.example.asb.faults.chart.ChartActivity
import com.example.asb.faults.chart.ChartHelper
import com.example.asb.mqtt.MqttCallbackHandler
import com.example.asb.mqtt.MqttProductionHelper
import com.example.asb.topic.MqttTopicManager
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FaultsActivity : AppCompatActivity(), MqttCallbackHandler {
    private lateinit var mqttProductionHelper: MqttProductionHelper
    private lateinit var adapter: AlarmAdapter
    private var allAlarms: List<Alarma> = emptyList()
    private lateinit var chartHelper: ChartHelper
    private lateinit var mqttTopicRequest: String
    private lateinit var mqttTopicResponse: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faults)

        // 1. Obtener parámetros dinámicos
        val clientId = intent.getStringExtra("CLIENT_ID") ?: "client_default"
        val projectId = intent.getStringExtra("WORK_ORDER") ?: "project_default"

        // 2. Generar tópicos usando MqttTopicManager
        mqttTopicRequest = MqttTopicManager.getAlarmsRequestTopic(clientId, projectId)
        mqttTopicResponse = MqttTopicManager.getAlarmsResponseTopic(clientId, projectId)

        Log.d("FAULTS", "Tópico Request: $mqttTopicRequest")
        Log.d("FAULTS", "Tópico Response: $mqttTopicResponse")

        // 3. Configurar MQTT de producción
        mqttProductionHelper = MqttProductionHelper(this).apply { connect() }

        //inicializar chart
        chartHelper = ChartHelper()

        // En onCreate():
        findViewById<Button>(R.id.btn_bomba1).isEnabled = true
        findViewById<Button>(R.id.btn_bomba2).isEnabled = true
        findViewById<Button>(R.id.btn_show_chart).visibility = View.GONE

        val rvAlarms = findViewById<RecyclerView>(R.id.rv_alarms)
        rvAlarms.layoutManager = LinearLayoutManager(this)
        adapter = AlarmAdapter(emptyList())
        rvAlarms.adapter = adapter

        // Botón "Bomba 1"
        findViewById<Button>(R.id.btn_bomba1).setOnClickListener {
            if (allAlarms.isNotEmpty()) {
                adapter.updateAlarms(allAlarms.filter { it.mensaje.contains("BOMBA 1", ignoreCase = true) })
                findViewById<TextView>(R.id.tv_instruccion).visibility = View.GONE // <- Ocultar instrucción al filtrar
            }
        }

        // Botón "Bomba 2"
        findViewById<Button>(R.id.btn_bomba2).setOnClickListener {
            if (allAlarms.isNotEmpty()) {
                adapter.updateAlarms(allAlarms.filter { it.mensaje.contains("BOMBA 2", ignoreCase = true) })
                findViewById<TextView>(R.id.tv_instruccion).visibility = View.GONE // <- Ocultar instrucción al filtrar
            }
        }
    }

    // 3. Publicar petición al presionar el botón
    private fun requestAlarms() {
        mqttProductionHelper.publish(
            topic = mqttTopicRequest, // Usar tópico dinámico
            message = "get_alarms"
        )
    }

    // 4. Manejar respuesta de Node-Red
    override fun onMessageReceived(topic: String, message: String) {
        Log.d("FAULTS", "Mensaje recibido en tópico: $topic")
        if (topic == mqttTopicResponse) {
            Log.d("FAULTS", "Contenido del mensaje: $message")
            runOnUiThread {
                allAlarms = parseAlarms(message)
                adapter.updateAlarms(allAlarms)

                // 1. Habilitar botones de filtro
                findViewById<Button>(R.id.btn_bomba1).isEnabled = true
                findViewById<Button>(R.id.btn_bomba2).isEnabled = true

                // 2. Mostrar y configurar botón del gráfico
                // Configurar botón del gráfico
                findViewById<Button>(R.id.btn_show_chart).apply {
                    // Mostrar el botón solo si hay alarmas
                    visibility = if (allAlarms.isNotEmpty()) View.VISIBLE else View.GONE
                    isEnabled = allAlarms.isNotEmpty()

                    setOnClickListener {
                        if (allAlarms.isNotEmpty()) {
                            try {
                                // Preparamos los datos para el gráfico
                                val chartJson = Gson().toJson(
                                    AlarmasResponse(
                                        cliente = "client2",  // Puedes obtener esto de tus extras
                                        equipo = "Svv",
                                        data = AlarmasData(
                                            alarmas = allAlarms,
                                            total = allAlarms.size,
                                            lastUpdate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                                                .format(Date())
                                        )
                                    )
                                )

                                Log.d("CHART_DATA", "JSON para gráfico: $chartJson")

                                val chartData = chartHelper.getChartData(chartJson)
                                Log.d("CHART_DATA", "Datos procesados: ${chartData.size} elementos")

                                val intent = Intent(this@FaultsActivity, ChartActivity::class.java).apply {
                                    putExtra("chartData", ArrayList(chartData))
                                }
                                startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("CHART_ERROR", "Error al preparar gráfico", e)
                                Toast.makeText(
                                    this@FaultsActivity,
                                    "Error al generar gráfico",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }
    // Reemplaza el metodo parseAlarms
    private fun parseAlarms(rawData: String): List<Alarma> {
        Log.d("FAULTS", "JSON recibido: $rawData")
        return try {
            val response = Gson().fromJson(rawData, AlarmasResponse::class.java)
            response.data.alarmas.sortedByDescending { parseDate(it.fecha) } // Ordena por fecha (ajusta si usas timestamp)
        } catch (e: Exception) {
            Log.e("FAULTS", "Error al parsear JSON", e)
            emptyList()
        }
    }

    private fun parseDate(dateString: String): Long {
        return try {
// Formato ISO 8601 con timezone (ej: "2025-07-02T18:57:08.351Z")
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC") // Asegura que interprete la 'Z' como UTC
            format.parse(dateString)?.time ?: 0        } catch (e: Exception) {
            Log.e("FAULTS", "Error al parsear fecha: $dateString", e)
            0  // En caso de error, se ordenará al final
        }
    }

    override fun onConnectionLost(cause: Throwable) {
        Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show()
    }

    override fun onConnectionSuccess() {
        Log.d("FAULTS", "Conectado a MQTT")
        mqttProductionHelper.subscribe(mqttTopicResponse)
        requestAlarms()
    }

    override fun onDestroy() {
        mqttProductionHelper.unsubscribe(mqttTopicResponse)
        mqttProductionHelper.disconnect()
        super.onDestroy()
    }
}