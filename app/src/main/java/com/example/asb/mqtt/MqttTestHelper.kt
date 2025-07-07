package com.example.asb.mqtt

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttTestHelper(private val callback: MqttCallbackHandler) {
    private val testBrokerUrl = "tcp://broker.hivemq.com:1883"
    private var mqttClient: MqttAsyncClient? = null
    private var subscribedTopics = mutableSetOf<String>()

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

    fun subscribe(topic: String) {
        Log.d("MQTT_DEBUG", "=== Intentando suscribir a [$topic] ===")
        Log.d("MQTT_DEBUG", "Estado conexión: ${isConnected()}")
        if (!subscribedTopics.contains(topic)) {
            Log.d("MQTT_DEBUG", "Intentando suscribir a $topic. Estado conexión: ${isConnected()}")
            mqttClient?.subscribe(topic, 1)?.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT_DEBUG", "🔔 Suscrito EXITOSAMENTE a $topic")
                }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT_DEBUG", "❌ Error al suscribir a $topic: ${exception?.message}")
                }
            }
            subscribedTopics.add(topic)
        }
    }

    fun unsubscribe(topic: String) {
        if (subscribedTopics.contains(topic)) {
            try {
                mqttClient?.unsubscribe(topic)?.actionCallback = object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("MQTT_DEBUG", "✅ Desuscripción exitosa de $topic")
                    }
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e("MQTT_DEBUG", "❌ Error al desuscribir de $topic: ${exception?.message}")
                    }
                }
                subscribedTopics.remove(topic)
            } catch (e: Exception) {
                Log.e("MQTT_DEBUG", "💥 Excepción en unsubscribe(): ${e.message}")
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
    fun publish(topic: String, message: String) { //Para la parte de alarmas
        try {
            mqttClient?.publish(topic, MqttMessage(message.toByteArray()))
            Log.d("MQTT_TEST", "📤 Mensaje publicado en [$topic]")
        } catch (e: Exception) {
            Log.e("MQTT_TEST", "❌ Error al publicar: ${e.message}")
        }
    }
    private fun isConnected(): Boolean {
        return mqttClient?.isConnected ?: false
    }
}