package com.example.asb.mqtt

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttTestHelper(private val callback: MqttCallbackHandler) {
    private val testBrokerUrl = "tcp://broker.hivemq.com:1883"
    private val testTopic = "003/0004/01/02/Datos"
    private var mqttClient: MqttAsyncClient? = null

    fun connect() {
        try {
            mqttClient = MqttAsyncClient(
                testBrokerUrl,
                "TestClient_${System.currentTimeMillis()}",
                MemoryPersistence()
            ).apply {
                setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        Log.e("MQTT_TEST", "Conexión perdida: ${cause?.message}")
                        callback.onConnectionLost(cause ?: Throwable("Error desconocido"))
                    }

                    override fun messageArrived(topic: String, message: MqttMessage) {
                        val payload = String(message.payload)
                        Log.d("MQTT_TEST", "Mensaje recibido: $topic - $payload")
                        callback.onMessageReceived(topic, payload)
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        Log.d("MQTT_TEST", "Mensaje entregado al broker: ${token?.messageId}")
                    }
                })

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    isAutomaticReconnect = true
                    connectionTimeout = 30
                    keepAliveInterval = 60
                }

                connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("MQTT_TEST", "✅ Conectado a $testBrokerUrl")
                        subscribe(testTopic)
                        callback.onConnectionSuccess()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e("MQTT_TEST", "❌ Error de conexión: ${exception?.message}")
                        callback.onConnectionLost(exception ?: Throwable("Error genérico"))
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("MQTT_TEST", "Error en connect(): ${e.message}")
            callback.onConnectionLost(e)
        }
    }

    private fun subscribe(topic: String) {
        mqttClient?.subscribe(topic, 1)
    }

    fun disconnect() {
        mqttClient?.disconnect()?.actionCallback = object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT_TEST", "Desconectado correctamente")
            }
            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT_TEST", "Error al desconectar")
            }
        }
    }
}