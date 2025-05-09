package com.example.asb.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.asb.MainActivity
import com.example.asb.databinding.ActivitySelectWorkOrderBinding
import com.example.asb.network.ApiClient
import com.example.asb.network.WorkOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class SelectWorkOrderActivity : AppCompatActivity() {
    // Binding para vistas
    private lateinit var binding: ActivitySelectWorkOrderBinding
    // Adaptador para el spinner
    private lateinit var adapter: ArrayAdapter<String>
    // Lista de órdenes de trabajo
    private val workOrders = mutableListOf<WorkOrder>()
    // Contador de reintentos
    private var retryCount = 0
    // Máximo de reintentos
    private val maxRetries = 3

    companion object {
        const val BASE_TOPIC = "asb/telemetria"
        const val TOPIC_SUFFIX = "operaciones/bombas/data"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Infla el layout usando view binding
        binding = ActivitySelectWorkOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtiene token del intent o termina si no es válido
        val token = intent.getStringExtra("TOKEN") ?: run {
            showToast("Token no válido")
            finish()
            return
        }

        // Obtiene ID de cliente del intent o termina si no es válido
        val idCliente = intent.getStringExtra("ID_CLIENTE") ?: run {
            showToast("ID de cliente no válido")
            finish()
            return
        }

        // Configura la UI y carga las órdenes de trabajo
        setupUI()
        loadWorkOrders(token, idCliente)
    }

    private fun setupUI() {
        // Configura el adaptador para el AutoCompleteTextView
        adapter = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerWorkOrders.setAdapter(adapter)

        // Configura el listener del botón Continuar
        binding.btnContinue.setOnClickListener {
            if (workOrders.isNotEmpty() && binding.spinnerWorkOrders.text.isNotEmpty()) {
                val selectedName = binding.spinnerWorkOrders.text.toString()
                val selected = workOrders.find { it.name == selectedName } ?: run {
                    showToast("Orden no válida")
                    return@setOnClickListener
                }

                // Obtén el ID del cliente del intent
                val clientId = intent.getStringExtra("ID_CLIENTE") ?: "client_default"

                // Genera el tópico dinámico
                val mqttTopic = "$BASE_TOPIC/$clientId/${selected.id}/$TOPIC_SUFFIX"
                Log.d("MQTT_TOPIC", "Tópico generado: $mqttTopic")

                // Inicia MainActivity con todos los datos necesarios
                startActivity(Intent(this, MainActivity::class.java).apply {
                    putExtra("WORK_ORDER", selected.id.toString())
                    putExtra("PROJECT_NAME", selected.name)
                    putExtra("CLIENT_ID", clientId)  // <-- Añade esto
                    putExtra("MQTT_TOPIC", mqttTopic)  // <-- Nuevo: envía el tópico ya generado
                })
            }
        }
    }

    // Actualiza la lista de órdenes en el spinner
    private fun updateOrdersList(orders: List<WorkOrder>) {
        adapter.clear()
        adapter.addAll(orders.map { it.name })
        workOrders.clear()
        workOrders.addAll(orders)

        // Habilita/deshabilita botón según haya órdenes
        binding.btnContinue.isEnabled = orders.isNotEmpty()

        // Muestra/oculta mensaje de lista vacía
        if (orders.isEmpty()) {
            binding.cardEmpty.visibility = View.VISIBLE
        } else {
            binding.cardEmpty.visibility = View.GONE
        }
    }

    // Carga las órdenes de trabajo desde la API
    private fun loadWorkOrders(token: String, idCliente: String) {
        Log.d("SelectWorkOrder", "Iniciando carga de órdenes de trabajo")
        if (retryCount >= maxRetries) {
            showToast("No se pudo cargar las órdenes")
            return
        }

        // Muestra progress bar y deshabilita botón
        binding.progressBar.visibility = View.VISIBLE
        binding.btnContinue.isEnabled = false

        // Lanza corrutina en background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Realiza petición con timeout de 30 segundos
                val response = withTimeout(30_000) {
                    ApiClient.apiService.getWorkOrders("Bearer $token", idCliente)
                }

                // Procesa respuesta en hilo principal
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // Si hay éxito, actualiza lista o muestra mensaje si está vacía
                        response.body()?.data?.let { orders ->
                            updateOrdersList(orders)
                        } ?: showToast("No hay órdenes disponibles")
                    } else {
                        // Maneja error de respuesta
                        handleError("Error: ${response.code()}", token, idCliente)
                    }
                }
            } catch (e: Exception) {
                // Maneja error de conexión
                withContext(Dispatchers.Main) {
                    handleError("Error de conexión", token, idCliente)
                }
            } finally {
                // Oculta progress bar al finalizar
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    // Maneja errores con reintentos
    private fun handleError(message: String, token: String, idCliente: String) {
        showToast(message)
        retryCount++
        if (retryCount < maxRetries) {
            // Reintenta después de 2 segundos
            binding.root.postDelayed({
                loadWorkOrders(token, idCliente)
            }, 2000)
        }
    }

    // Muestra toast con mensaje
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpia callbacks al destruir la actividad
        binding.root.removeCallbacks(null)
    }
}