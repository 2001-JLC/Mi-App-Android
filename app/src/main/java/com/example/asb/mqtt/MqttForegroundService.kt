package com.example.asb.mqtt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.asb.R
import com.example.asb.faults.notification.MqttNotificationManager

//estaclase interactua con MqttNotificationManager.kt
//MqttForegroundService es independiente de las Activities y sobrevive en segundo plano.
class MqttForegroundService : Service() {
    // Instancia de MqttTestHelper para manejar la conexión MQTT
    private lateinit var mqttHelper: MqttTestHelper
    // Manager para notificaciones, inicializado lazy para optimización
    private val notificationManager by lazy { MqttNotificationManager(this) }

    override fun onCreate() {
        super.onCreate()
        // Configuración del helper MQTT con callbacks
        mqttHelper = MqttTestHelper(object : MqttCallbackHandler {
            // Cuando llega un mensaje MQTT, lo redirige al NotificationManager según el tópico
            override fun onMessageReceived(topic: String, message: String) {
                when (topic) {
                    "ASBOMBEO/DEMO/ALARMA/REGISTRO" -> notificationManager.setRegistro(message)
                    "ASBOMBEO/DEMO/ALARMA/MENSAJE" -> notificationManager.setMensaje(message)
                    "ASBOMBEO/DEMO/ALARMA/FECHA" -> notificationManager.setFecha(message)
                }
            }
            override fun onConnectionSuccess() = Unit // Sin acción necesaria
            override fun onConnectionLost(cause: Throwable) = Unit // Sin acción necesaria
        })

        mqttHelper.connect() // Inicia la conexión MQTT
        startForeground(NOTIFICATION_ID, createNotification()) // Servicio en primer plano con notificación
    }

    // Crea la notificación persistente del servicio
    private fun createNotification(): Notification {
        val channelId = "alarmas_channel"

        // Crea el canal de notificación (requerido para Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(channelId, "Notificaciones de Alarmas")
        }

        // Construye la notificación con:
        // - Título y texto descriptivo
        // - Icono pequeño (requerido)
        // - Prioridad baja (no molesta al usuario)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Monitor de Alarmas Activo")
            .setContentText("Escuchando nuevas alarmas...")
            .setSmallIcon(R.mipmap.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // Crea el canal de notificación (solo para Android 8+)
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, name: String) {
        val channel = NotificationChannel(
            channelId,
            name,
            NotificationManager.IMPORTANCE_LOW // Prioridad baja para no molestar
        ).apply {
            description = "Canal para notificaciones de alarmas"
        }

        // Registra el canal en el sistema
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?) = null // Servicio no vinculado

    companion object {
        private const val NOTIFICATION_ID = 1 // ID fijo para la notificación del servicio
    }
}