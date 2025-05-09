package com.example.asb.mqtt

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttClientManager(
    private val serverUri: String,
    private val clientId: String = "AndroidClient_${System.currentTimeMillis()}"
) {
    private var mqttClient: MqttAsyncClient? = null
    private var callback: MqttCallbackHandler? = null

    fun connect(callback: (Boolean) -> Unit) {
        try {
            mqttClient = MqttAsyncClient(serverUri, clientId, MemoryPersistence()).apply {
                setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        Log.e("MQTT", "Connection lost", cause)
                        this@MqttClientManager.callback?.onConnectionLost(cause ?: Throwable("Unknown error"))
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        if (topic != null && message != null) {
                            this@MqttClientManager.callback?.onMessageReceived(topic, String(message.payload))
                            Log.d("MQTT", "Message arrived on $topic: ${String(message.payload)}")
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        Log.d("MQTT", "Message delivered")
                    }
                })

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    isAutomaticReconnect = true
                    connectionTimeout = 60 // Reduce el timeout para debug
                    keepAliveInterval = 60
                }

                connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("MQTT", "Connection success")
                        this@MqttClientManager.callback?.onConnectionSuccess()
                        callback(true)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e("MQTT", "Connection failed", exception)
                        this@MqttClientManager.callback?.onConnectionLost(exception ?: Throwable("Connection failed"))
                        callback(false) // Asegúrate de que esto se ejecute
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("MQTT", "Error connecting", e)
            callback(false)
        }
    }

    fun subscribe(topic: String, qos: Int = 1) {
        try {
            mqttClient?.subscribe(topic, qos)?.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Subscribe failed", exception)
                }
            }
        } catch (e: Exception) {
            Log.e("MQTT", "Subscribe error", e)
        }
    }

    fun setCallback(handler: MqttCallbackHandler) {
        this.callback = handler
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()?.waitForCompletion()
        } catch (e: Exception) {
            Log.e("MQTT", "Disconnect error", e)
        }
    }
}