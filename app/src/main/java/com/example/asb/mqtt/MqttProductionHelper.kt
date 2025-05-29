package com.example.asb.mqtt

import android.os.Handler
import android.os.Looper
import android.util.Log

// Clase helper para gestionar la conexión MQTT con el broker oficial
class MqttProductionHelper(private val mainHandler: MqttCallbackHandler) {
    private val productionBrokerUrl = "ws://asbombeo.ddns.net:8083/mqtt"
    private lateinit var productionMqttManager: MqttClientManager

    @Volatile private var currentTopic: String? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 3

    private val internalCallback = object : MqttCallbackHandler {
        override fun onMessageReceived(topic: String, message: String) {
            Log.d("MQTT_PROD", "Mensaje recibido: $topic - $message")
            mainHandler.onMessageReceived(topic, message)
        }

        override fun onConnectionLost(cause: Throwable) {
            Log.e("MQTT_PROD", "Conexión perdida", cause)
            mainHandler.onConnectionLost(cause)
        }

        override fun onConnectionSuccess() {
            Log.d("MQTT_PROD", "¡Conectado al broker oficial!")
            mainHandler.onConnectionSuccess()
        }
    }
    // Inicia la conexión con el broker oficial
    fun startProductionConnection(topic: String? = null) {
        currentTopic = topic

        if (!::productionMqttManager.isInitialized) {
            productionMqttManager = MqttClientManager.getInstance(productionBrokerUrl)
        }

        productionMqttManager.removeCallback(internalCallback)
        productionMqttManager.addCallback(internalCallback)

        if (productionMqttManager.isConnected()) {
            currentTopic?.let { subscribeIfNeeded(it) }
            return
        }

        productionMqttManager.connect { success ->
            if (success) {
                reconnectAttempts = 0 // Resetear intentos
                currentTopic?.let { subscribeIfNeeded(it) }
            } else if (reconnectAttempts < maxReconnectAttempts) {
                reconnectAttempts++
                Handler(Looper.getMainLooper()).postDelayed({
                    startProductionConnection(currentTopic)
                }, 5000)
            } else {
                Log.e("MQTT_PROD", "Máximo de reintentos alcanzado")
            }
        }
    }

    // Suscribe al tópico especificado
    private fun subscribeToTopic(topic: String, callback: ((Boolean) -> Unit)? = null) {
        productionMqttManager.subscribe(topic) { success ->
            if (success) {
                // Actualiza el tópico actual si la suscripción fue exitosa
                currentTopic = topic
                Log.d("MQTT_PROD", "Suscrito a: $topic")
            }
            // Ejecuta el callback opcional con el resultado
            callback?.invoke(success)
        }
    }
    private fun subscribeIfNeeded(topic: String) {
        if (!productionMqttManager.hasSubscription(topic)) { // ¡Nuevo método necesario en MqttClientManager!
            subscribeToTopic(topic)
        }
    }
}