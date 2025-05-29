package com.example.asb.mqtt

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

// Clase principal para gestionar la conexión MQTT usando el patrón Singleton
class MqttClientManager private constructor(

    private val serverUri: String,  // URL del broker MQTT
    private var clientId: String = "AndroidClient_${serverUri.hashCode()}_${System.currentTimeMillis()}" // ID único del cliente
) {
    private var mqttClient: MqttAsyncClient? = null// Cliente MQTT subyacente
    private val callbacks = CopyOnWriteArrayList<MqttCallbackHandler>()  // Lista de callbacks registrados
    private val subscribedTopics = ConcurrentHashMap<String, Boolean>().keySet(true)

    companion object {
        private val instances = mutableMapOf<String, MqttClientManager>()

        fun getInstance(serverUri: String): MqttClientManager {
            return instances[serverUri] ?: synchronized(this) {
                instances[serverUri] ?: MqttClientManager(serverUri).also {
                    instances[serverUri] = it
                }
            }
        }

    }

    // 2. Nueva función para verificar callbacks
    fun hasCallback(handler: MqttCallbackHandler): Boolean {
        return callbacks.contains(handler)
    }

    // Método para establecer la conexión MQTT
    fun connect(callback: (Boolean) -> Unit) {
        try {
            if (mqttClient?.isConnected == true && mqttClient?.serverURI == serverUri) {
                Log.d("MQTT", "✅ Ya conectado a $serverUri")
                callback(true)
                return
            }
            // Desconexión segura
            mqttClient?.disconnectForcibly(500, 1000) // Timeout de 1s

            // Crea una nueva instancia del cliente MQTT
            mqttClient = MqttAsyncClient(serverUri, clientId, MemoryPersistence()).apply {
                // Configura los callbacks MQTT
                setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        Log.e("MQTT", "🔴 Conexión perdida: ${cause?.message ?: "Sin razón"}")
                        // Notifica a todos los callbacks registrados
                        callbacks.forEach { it.onConnectionLost(cause ?: Throwable("Unknown error")) }
                    }

                    override fun messageArrived(topic: String, message: MqttMessage) {
                        Log.d("MQTT", "📬 Mensaje recibido [$topic]")
                        // Convierte el payload a String y notifica a los callbacks
                        val payload = String(message.payload)
                        callbacks.forEach { it.onMessageReceived(topic, payload) }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken) {
                        Log.d("MQTT", "📪 Mensaje entregado")
                        // Notifica éxito de conexión
                        callbacks.forEach { it.onConnectionSuccess() }
                    }
                })
                // 4. Configuración optimizada de opciones
                val options = MqttConnectOptions().apply {
                    isCleanSession = true       // Sesiones limpias (¡clave para Android!)
                    isAutomaticReconnect = true // Reconexión automática
                    connectionTimeout = 30      // 30 segundos para timeout
                    keepAliveInterval = 60      // 60 segs entre pings
                    maxReconnectDelay = 3000    // 3 segs entre reintentos
                }

                // 5. Conexión con logs detallados
                Log.d("MQTT", "🔄 Conectando a $serverUri...")
                connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("MQTT", "✅ Conexión exitosa a $serverUri")
                        callbacks.forEach { it.onConnectionSuccess() }
                        callback(true)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val errorMsg = exception?.message ?: "Error desconocido"
                        Log.e("MQTT", "❌ Fallo de conexión: $errorMsg")
                        callbacks.forEach { it.onConnectionLost(exception ?: Throwable(errorMsg)) }
                        callback(false)
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("MQTT", "Connect error", e)
            callback(false)
        }
    }

    // Registra un nuevo callback
    fun addCallback(handler: MqttCallbackHandler) {
        if (!callbacks.contains(handler)) {
            callbacks.add(handler)
            Log.d("MQTT", "Callback añadido: ${handler.javaClass.simpleName}")
        }
    }

    fun removeCallback(handler: MqttCallbackHandler) {  // ✅ Cambiado de MqttRepository a MqttCallbackHandler
        callbacks.remove(handler)
        Log.d("MQTT", "Callback removido: ${handler.javaClass.simpleName}")
    }

    // Suscribe a un tópico MQTT
    fun subscribe(topic: String, callback: ((Boolean) -> Unit)? = null) {
        try {
            mqttClient?.subscribe(topic, 1)?.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    subscribedTopics.add(topic) // Registrar éxito
                    callback?.invoke(true)
                }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    callback?.invoke(false)
                }
            }
        } catch (e: Exception) {
            callback?.invoke(false)
        }
    }


    // Verifica si un tópico está suscrito
    fun hasSubscription(topic: String): Boolean {
        return subscribedTopics.contains(topic)
    }

    fun isConnected(): Boolean = mqttClient?.isConnected == true

    // Desconecta el cliente MQTT
    fun disconnect() {
        try {
            mqttClient?.disconnect() // Intento normal primero
                ?.waitForCompletion(1000) // Espera 1s
            mqttClient?.disconnectForcibly(1, 1000) // Fallback
        } catch (e: Exception) {
            Log.e("MQTT", "Disconnect error", e)
        } finally {
        }
    }
}