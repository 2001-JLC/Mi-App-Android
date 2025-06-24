package com.example.asb.mqtt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.asb.R
import com.example.asb.faults.notification.MqttNotificationManager

/**
 * Servicio en primer plano que mantiene la conexión MQTT activa.
 *
 * Interactúa con:
 * - MqttTestHelper: Para manejar la conexión MQTT
 * - MqttNotificationManager: Para mostrar notificaciones de alarmas
 *
 * Sobrevive en segundo plano incluso si la app está cerrada.
 */
class MqttForegroundService : Service() {

    // Instancia para manejar la conexión MQTT
    private lateinit var mqttHelper: MqttTestHelper

    // Manager de notificaciones (inicializado solo cuando se usa por primera vez)
    private val notificationManager by lazy { MqttNotificationManager(this) }

    // Estado actual de la conexión
    private var isConnected = false

    // Handler para manejar reconexiones automáticas
    private val reconnectHandler = Handler(Looper.getMainLooper())
    // 1. Tópico constante para notificaciones (según lo planeado)
    private val notificationTopic = "asb/telemetria/client2/proyect291/alarmas/notification"

    override fun onCreate() {
        super.onCreate()

        // Configuración del cliente MQTT
        mqttHelper = MqttTestHelper(object : MqttCallbackHandler {
            /**
             * Cuando llega un mensaje MQTT:
             * - Filtra por tópico
             * - Envia los datos al NotificationManager
             */
            override fun onMessageReceived(topic: String, message: String) {
                when (topic) {
                    notificationTopic -> {
                        //Procesar el json completo
                        notificationManager.showNotificationFromJson(message)
                    }
                }
            }

            /**
             * Conexión exitosa:
             * - Actualiza estado
             * - Modifica la notificación
             */
            override fun onConnectionSuccess() {
                isConnected = true
                updateNotification("Conexión MQTT activa")
            }

            /**
             * Pérdida de conexión:
             * - Actualiza estado
             * - Notifica al usuario
             * - Programa reconexión automática en 5 segundos
             */
            override fun onConnectionLost(cause: Throwable) {
                isConnected = false
                updateNotification("Conexión perdida - Reconectando...")

                reconnectHandler.postDelayed({
                    if (!isConnected) {
                        mqttHelper.connect() // Intento de reconexión
                    }
                }, 5000)
            }
        })

        // Inicia la conexión y muestra notificación inicial
        mqttHelper.connect()
        startForeground(NOTIFICATION_ID, createNotification("Conectando..."))
    }

    /**
     * Actualiza el texto de la notificación del servicio.
     * @param message Nuevo mensaje a mostrar
     */
    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }

    /**
     * Crea la notificación del servicio con estado dinámico.
     * @param status Texto del estado (valor por defecto para primera creación)
     */
    private fun createNotification(status: String = "Escuchando nuevas alarmas..."): Notification {
        // Crea el canal (requerido para Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("alarmas_channel", "Notificaciones de Alarmas")
        }

        return NotificationCompat.Builder(this, "alarmas_channel")
            .setContentTitle("Monitor de Alarmas") // Título fijo
            .setContentText(status)               // Estado dinámico
            .setSmallIcon(R.mipmap.ic_notification) // Icono obligatorio
            .setPriority(NotificationCompat.PRIORITY_LOW) // Baja prioridad
            .setOngoing(true) // Notificación no deslizable
            .build()
    }

    /**
     * Crea el canal de notificaciones (Android 8+ Oreo)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, name: String) {
        NotificationChannel(
            channelId,
            name,
            NotificationManager.IMPORTANCE_LOW // No molesta al usuario
        ).apply {
            description = "Canal para notificaciones de alarmas"
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpia los callbacks de reconexión al destruir el servicio
        reconnectHandler.removeCallbacksAndMessages(null)
    }

    // Servicio no vinculado (no se comunica con componentes)
    override fun onBind(intent: Intent?) = null

    companion object {
        // ID único para la notificación del servicio
        private const val NOTIFICATION_ID = 1
    }
}