package com.example.asb.faults.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.asb.R
import com.example.asb.faults.FaultsActivity
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlin.random.Random

//estaclase interactua con MqttForegroundService.kt
class MqttNotificationManager(private val context: Context) {

    data class NotificacionAlarma(
        val status: String,
        val timestamp: String,
        val alarma: AlarmaDetalle
    )

    data class AlarmaDetalle(
        val id: Int,
        val codigo: Int,
        val mensaje: String,
        @SerializedName("fecha_original") val fechaOriginal: String,
        @SerializedName("timestamp_unix") val timestampUnix: Long,
        val prioridad: String // "alta", "media", "baja"
    )

    fun showNotificationFromJson(json: String) {
        try {
            val notificacion = Gson().fromJson(json, NotificacionAlarma::class.java)
            showNotification(alarma = notificacion.alarma)
        } catch (e: Exception) {
            Log.e("NotificationManager", "Error al parsear JSON", e)
        }
    }

    // Método actualizado para recibir AlarmaDetalle como parámetro
    private fun showNotification(alarma: AlarmaDetalle) { // <- ¡Parámetro añadido aquí!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, FaultsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construye la notificación con:
        // - Título: mensaje de la alarma
        // - Texto: registro + fecha
        // - Icono e intención al hacer clic
        // - Prioridad alta (para que sea más visible)
        val notification = NotificationCompat.Builder(context, "alarmas_channel")
            .setContentTitle(alarma.mensaje) // Ahora alarma es el parámetro
            .setContentText("Código ${alarma.codigo} - ${alarma.fechaOriginal}")
            .setSmallIcon(R.mipmap.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(convertPriority(alarma.prioridad))
            .build()

        NotificationManagerCompat.from(context).notify(Random.nextInt(), notification)
    }

    private fun convertPriority(prioridad: String): Int {
        return when (prioridad.lowercase()) {
            "alta" -> NotificationCompat.PRIORITY_HIGH
            "media" -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }
    }
}