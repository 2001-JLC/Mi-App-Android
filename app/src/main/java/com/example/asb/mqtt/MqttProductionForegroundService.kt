package com.example.asb.mqtt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.asb.R
import com.example.asb.faults.notification.MqttNotificationManager
import com.example.asb.faults.notification.SharedPrefHelper
import com.example.asb.topic.MqttTopicManager
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken

class MqttProductionForegroundService : Service() {

    private lateinit var mqttHelper: MqttProductionHelper
    private var isSubscribed = false
    private val notificationManager by lazy { MqttNotificationManager(this) }
    private var isConnected = false
    private val reconnectHandler = Handler(Looper.getMainLooper())
    private lateinit var alarmTopic: String
    // Variables para almacenar los parámetros
    private var clientId: String = "client_default"
    private var projectId: String = "project_default"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val newClientId = it.getStringExtra("CLIENT_ID") ?: "client_default"
            val newProjectId = it.getStringExtra("WORK_ORDER") ?: "project_default"

            if (newClientId != clientId || newProjectId != projectId) {
                clientId = newClientId
                projectId = newProjectId
                alarmTopic = MqttTopicManager.getAlarmsNotificationTopic(clientId, projectId)
                Log.d("MQTT_TOPIC", "Tópico actualizado: $alarmTopic")

                // Solo reconectar si ya está inicializado
                if (::mqttHelper.isInitialized) {
                    mqttHelper.disconnect()
                    setupMqtt()
                }
            }
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        alarmTopic = MqttTopicManager.getAlarmsNotificationTopic(clientId, projectId)
        setupMqtt()
        startForeground(NOTIFICATION_ID, createNotification("Conectando a producción..."))
    }

    private fun setupMqtt() {
        mqttHelper = MqttProductionHelper(object : MqttCallbackHandler {
            override fun onMessageReceived(topic: String, message: String) {
                if (topic == alarmTopic) {
                    notificationManager.showNotificationFromJson(message)
                    SharedPrefHelper.saveLastAlarm(this@MqttProductionForegroundService, message)
                }
            }

            override fun onConnectionSuccess() {
                isConnected = true
                mqttHelper.subscribe(alarmTopic, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        isSubscribed = true
                        Log.d("MQTT", "✅ Suscripción exitosa a $alarmTopic")
                        // Recrear canal de notificaciones
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            (getSystemService(NotificationManager::class.java)?.apply {
                                deleteNotificationChannel("alarmas_channel")
                            })
                        }
                        updateNotification("Conexión activa")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        isSubscribed = false
                        Log.e("MQTT", "❌ Error en suscripción: ${exception?.message}")
                        scheduleReconnect()
                    }
                })
            }

            override fun onConnectionLost(cause: Throwable) {
                isConnected = false
                isSubscribed = false
                updateNotification("Desconectado - Reconectando...")
                scheduleReconnect()
            }
        })
        mqttHelper.connect()
    }

    private fun scheduleReconnect() {
        reconnectHandler.postDelayed({
            if (!isConnected) {
                mqttHelper.connect()
            }
        }, 5000) // Reintenta cada 5 segundos
    }

    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(status: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("prod_channel", "Canal de Producción")
        }

        return NotificationCompat.Builder(this, "prod_channel")
            .setContentTitle("Monitor de Producción")
            .setContentText(status)
            .setSmallIcon(R.mipmap.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, name: String) {
        NotificationChannel(
            channelId,
            name,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificaciones del servicio MQTT de producción"
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        reconnectHandler.removeCallbacksAndMessages(null)
        mqttHelper.disconnect()
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        private const val NOTIFICATION_ID = 2 // ID diferente al servicio de test
    }
}