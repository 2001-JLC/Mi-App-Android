package com.example.asb.mqtt

import android.util.Log
//en este clase se uso de prueba para monitoriar villas garden con moquito
class MqttTestHelper(private val mainHandler: MqttCallbackHandler) {
    private val testBrokerUrl = "tcp://test.mosquitto.org:1883" // Broker público
    private val testTopic = "003/0004/01/02/Datos" // Tópico fijo para pruebas
    private lateinit var testMqttManager: MqttClientManager

    fun startTestConnection() {
        testMqttManager = MqttClientManager(testBrokerUrl, "TEST_${System.currentTimeMillis()}")
        testMqttManager.setCallback(object : MqttCallbackHandler {
            override fun onMessageReceived(topic: String, message: String) {
                Log.d("MQTT_TEST", "Mensaje recibido en $topic: $message")
                mainHandler.onMessageReceived(topic, message) // Reenvía a tu lógica principal
            }

            override fun onConnectionLost(cause: Throwable) {
                mainHandler.onConnectionLost(cause)
            }

            override fun onConnectionSuccess() {
                mainHandler.onConnectionSuccess()
            }
        })

        testMqttManager.connect { success ->
            if (success) {
                testMqttManager.subscribe(testTopic)
            } else {
                Log.e("MQTT_TEST", "Error al conectar al broker de prueba")
            }
        }
    }

    fun stopTestConnection() {
        testMqttManager.disconnect()
    }
}