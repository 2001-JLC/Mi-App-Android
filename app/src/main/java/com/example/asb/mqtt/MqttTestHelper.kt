package com.example.asb.mqtt

import android.os.Handler
import android.os.Looper
import android.util.Log

class MqttTestHelper(private val mainHandler: MqttCallbackHandler) {
    private val testBrokerUrl = "tcp://broker.hivemq.com:1883"
    private val testTopic = "003/0004/01/02/Datos"
    private lateinit var testMqttManager: MqttClientManager
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 3

    private val internalCallback = object : MqttCallbackHandler {
        override fun onMessageReceived(topic: String, message: String) {
            Log.d("MQTT_TEST", "Mensaje recibido: $topic - $message")
            mainHandler.onMessageReceived(topic, message)
        }

        override fun onConnectionLost(cause: Throwable) {
            mainHandler.onConnectionLost(cause)
        }

        override fun onConnectionSuccess() {
            mainHandler.onConnectionSuccess()
        }
    }

    fun startTestConnection() {
        testMqttManager = MqttClientManager.getInstance(testBrokerUrl).apply {
            removeCallback(internalCallback)
            addCallback(internalCallback)
        }

        if (testMqttManager.isConnected()) {
            testMqttManager.subscribe(testTopic)
            return
        }

        testMqttManager.connect { success ->
            if (success) {
                reconnectAttempts = 0
                testMqttManager.subscribe(testTopic)
            } else if (reconnectAttempts < maxReconnectAttempts) {
                reconnectAttempts++
                Handler(Looper.getMainLooper()).postDelayed({
                    startTestConnection()
                }, 5000)
            } else {
                Log.e("MQTT_TEST", "Error: Máximo de reintentos alcanzado")
            }
        }
    }

    fun stopTestConnection() {
        testMqttManager.removeCallback(internalCallback)
        testMqttManager.disconnect()
    }
}