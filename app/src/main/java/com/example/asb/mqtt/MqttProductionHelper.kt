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
) {
    private val brokerUrl = "ws://asbombeo.ddns.net:8083/mqtt"
    private var mqttClient: MqttAsyncClient? = null
    private var subscribedTopics = mutableSetOf<String>()

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

    fun subscribe(topic: String, callback: IMqttActionListener? = null) {
        Log.d("MQTT_DEBUG", "=== Intentando suscribir a [$topic] ===")
        if (!isConnected()) {  // <-- AQUÍ USAS EL MÉTODO
            callback?.onFailure(null, Exception("No conectado al broker"))
            return
        }

        if (!subscribedTopics.contains(topic)) {
            try {
                val token = mqttClient?.subscribe(topic, 1)
                token?.actionCallback = callback ?: object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("MQTT_DEBUG", "🔔 Suscrito EXITOSAMENTE a $topic")
                        subscribedTopics.add(topic)
                    }
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e("MQTT_DEBUG", "❌ Error al suscribir a $topic: ${exception?.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("MQTT_DEBUG", "💥 Error en subscribe(): ${e.message}")
                callback?.onFailure(null, e)
            }
        } else {
            callback?.onSuccess(null)
        }
    }

    fun unsubscribe(topic: String, callback: IMqttActionListener? = null) {
        if (subscribedTopics.contains(topic)) {
            try {
                val token = mqttClient?.unsubscribe(topic)
                token?.actionCallback = callback ?: object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        subscribedTopics.remove(topic)
                    }
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e("MQTT", "Error al desuscribir: ${exception?.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("MQTT", "Error en unsubscribe(): ${e.message}")
            }
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()?.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT_DEBUG", "🔌 Desconexión exitosa")
                }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT_DEBUG", "❌ Error al desconectar: ${exception?.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("MQTT_DEBUG", "💥 Excepción en disconnect(): ${e.message}")
        }
    }
    private fun isConnected(): Boolean {
        return mqttClient?.isConnected ?: false
    }

    // para la parte de faulstactivity
    fun publish(topic: String, message: String) {
        if (!isConnected()) {  // <-- AQUÍ TAMBIÉN
            Log.e("MQTT_PROD", "⚠️ No se puede publicar: desconectado")
            return
        }
        try {
            mqttClient?.publish(topic, MqttMessage(message.toByteArray()))
            Log.d("MQTT_PROD", "📤 Mensaje publicado en [$topic]")
        } catch (e: Exception) {
            Log.e("MQTT_PROD", "❌ Error al publicar: ${e.message}")
        }
    }
}