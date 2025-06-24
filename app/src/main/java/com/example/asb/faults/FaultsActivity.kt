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
import com.example.asb.mqtt.MqttTestHelper
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Locale

class FaultsActivity : AppCompatActivity(), MqttCallbackHandler {
    private lateinit var mqttHelper: MqttTestHelper
    private lateinit var adapter: AlarmAdapter
    private var allAlarms: List<Alarma> = emptyList()
    private lateinit var chartHelper: ChartHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faults)

        //inicializar chart
        chartHelper = ChartHelper()

        // En onCreate():
        findViewById<Button>(R.id.btn_bomba1).isEnabled = false
        findViewById<Button>(R.id.btn_bomba2).isEnabled = false
        findViewById<Button>(R.id.btn_show_chart).visibility = View.GONE

        val rvAlarms = findViewById<RecyclerView>(R.id.rv_alarms)
        rvAlarms.layoutManager = LinearLayoutManager(this)
        adapter = AlarmAdapter(emptyList())
        rvAlarms.adapter = adapter

        // MQTT
        mqttHelper = MqttTestHelper(this).apply { connect() }

        val tvInstruccion = findViewById<TextView>(R.id.tv_instruccion)

        // Botón "Todas" (unificado)
        findViewById<Button>(R.id.btn_all).setOnClickListener {
            requestAlarms()
            tvInstruccion.visibility = View.GONE
        }

        // Botón "Bomba 1"
        findViewById<Button>(R.id.btn_bomba1).setOnClickListener {
            if (allAlarms.isNotEmpty()) {
                adapter.updateAlarms(allAlarms.filter {
                    it.mensaje.contains("BOMBA 1", ignoreCase = true)
                })
            }
            tvInstruccion.visibility = View.GONE
        }

        // Botón "Bomba 2"
        findViewById<Button>(R.id.btn_bomba2).setOnClickListener {
            if (allAlarms.isNotEmpty()) {
                adapter.updateAlarms(allAlarms.filter {
                    it.mensaje.contains("BOMBA 2", ignoreCase = true)
                })
            }
            tvInstruccion.visibility = View.GONE
        }
    }

    // 3. Publicar petición al presionar el botón
    private fun requestAlarms() {
        mqttHelper.publish(
            topic = "ASBOMBEO/DEMO/ALARMA/PETICION",
            message = "get_alarms" // Mensaje puede ser cualquiera
        )
    }

    // 4. Manejar respuesta de Node-Red
    override fun onMessageReceived(topic: String, message: String) {
        if (topic == "ASBOMBEO/DEMO/ALARMA/ENVIO") {
            runOnUiThread {
                allAlarms = parseAlarms(message)
                adapter.updateAlarms(allAlarms)

                // 1. Habilitar botones de filtro
                findViewById<Button>(R.id.btn_bomba1).isEnabled = true
                findViewById<Button>(R.id.btn_bomba2).isEnabled = true

                // 2. Mostrar y configurar botón del gráfico
                // Configurar botón del gráfico
                findViewById<Button>(R.id.btn_show_chart).apply {
                    visibility = View.VISIBLE
                    isEnabled = true  // Asegurar que esté habilitado
                    setOnClickListener {
                        Log.d("FaultsActivity", "Botón gráfico clickeado")
                        if (allAlarms.isNotEmpty()) {
                            val intent =
                                Intent(this@FaultsActivity, ChartActivity::class.java).apply {
                                    putExtra(
                                        "chartData",
                                        ArrayList(chartHelper.getChartData(message))
                                    )
                                }
                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                this@FaultsActivity,
                                "No hay datos para graficar",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
    // Reemplaza el metodo parseAlarms
    private fun parseAlarms(rawData: String): List<Alarma> {
        Log.d("FAULTS", "Datos crudos: $rawData")
        return try {
            val response = Gson().fromJson(rawData, AlarmasResponse::class.java)
            response.alarmas.sortedByDescending { parseDate(it.fecha) } // Ordena por fecha (ajusta si usas timestamp)
        } catch (e: Exception) {
            Log.e("FAULTS", "Error al parsear JSON", e)
            emptyList()
        }
    }

    private fun parseDate(dateString: String): Long {
        return try {
            SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US).parse(dateString)?.time ?: 0
        } catch (e: Exception) {
            Log.e("FAULTS", "Error al parsear fecha: $dateString", e)
            0  // En caso de error, se ordenará al final
        }
    }

    override fun onConnectionLost(cause: Throwable) {
        Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show()
    }

    override fun onConnectionSuccess() {
        Log.d("FAULTS", "Conectado a MQTT")
        mqttHelper.subscribe("ASBOMBEO/DEMO/ALARMA/ENVIO")
    }

    override fun onDestroy() {
        mqttHelper.disconnect()
        super.onDestroy()
    }
}