package com.example.asb.mqtt

import android.util.Log
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttProductionHelper(
    private val callback: MqttCallbackHandler,
    private val topic: String
) {
    private val brokerUrl = "ws://asbombeo.ddns.net:8083/mqtt"
    private var mqttClient: MqttAsyncClient? = null

    fun connect() {
        try {
            mqttClient = MqttAsyncClient(
                brokerUrl,
                "ProdClient_${System.currentTimeMillis()}",
                MemoryPersistence()
            ).apply {
                setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        Log.e("MQTT_PROD", "❌ Conexión perdida: ${cause?.message}")
                        callback.onConnectionLost(cause ?: Throwable("Error desconocido"))
                    }

                    override fun messageArrived(topic: String, message: MqttMessage) {
                        Log.d("MQTT_PROD", "📬 Mensaje recibido [${topic}]: ${String(message.payload)}")
                        callback.onMessageReceived(topic, String(message.payload))
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    isAutomaticReconnect = true
                    connectionTimeout = 30
                    keepAliveInterval = 60
                }

                connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("MQTT_PROD", "✅ Conectado exitosamente a $brokerUrl")
                        subscribe(topic)
                        callback.onConnectionSuccess()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e("MQTT_PROD", "❌ Error en conexión: ${exception?.message}")
                        callback.onConnectionLost(exception ?: Throwable("Error genérico"))
                    }
                })
            }
        } catch (e: Exception) {
            callback.onConnectionLost(e)
        }
    }

    private fun subscribe(topic: String) {
        mqttClient?.subscribe(topic, 1)
    }

    fun disconnect() {
        mqttClient?.disconnect()
    }
}