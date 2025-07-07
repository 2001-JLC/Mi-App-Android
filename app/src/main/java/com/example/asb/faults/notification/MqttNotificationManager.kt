package com.example.asb.faults.notification

import android.app.NotificationChannel
import android.app.NotificationManager
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

class MqttNotificationManager(private val context: Context) {

    // 1. Nuevo modelo para el JSON de entrada (array de alarmas)
    data class Alarma(
        @SerializedName("id") val idModbus: Int,
        val mensaje: String,
        val fecha: String
    )

    // 2. Asignación de prioridades basadas en id_modbus
    private fun getPrioridad(idModbus: Int): String {
        return when (idModbus) {
            120, 121, 122, 123, 129 -> "alta"    // Paros de emergencia y fallas críticas
            124, 125, 126, 127, 128 -> "media"   // Fallas de arranque de bombas
            130, 131 -> "baja"                   // Voltaje bajo
            else -> "baja"                       // Cualquier otro ID no contemplado
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alarmas_channel",
                "Alarmas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para notificaciones de alarmas"
            }

            (context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel))
        }
    }

    // 3. Parsea el JSON y muestra notificaciones
    fun showNotificationFromJson(json: String) {
        try {
            ensureNotificationChannel()
            val alarmas = Gson().fromJson(json, Array<Alarma>::class.java).toList()
            if (alarmas.isNotEmpty()) {
                showGroupedNotification(alarmas) // Notificación agrupada
            }
        } catch (e: Exception) {
            Log.e("NotificationManager", "Error al parsear JSON", e)
        }
    }

    // 4. Notificación agrupada para múltiples alarmas
    private fun showGroupedNotification(alarmas: List<Alarma>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val groupKey = "alarmas_group"
        val notificationManager = NotificationManagerCompat.from(context)

        // Notificación individual para cada alarma (se agrupan)
        alarmas.forEach { alarma ->
            val builder = NotificationCompat.Builder(context, "alarmas_channel")
                .setContentTitle("${alarma.mensaje} ")
                .setContentText("Fecha: ${alarma.fecha}")
                .setSmallIcon(R.mipmap.ic_notification)
                .setGroup(groupKey)
                .setPriority(convertPriority(getPrioridad(alarma.idModbus)))
                .setAutoCancel(true)

            // Solo muestra notificación emergente si es prioridad ALTA
            if (getPrioridad(alarma.idModbus) == "alta") {
                builder.setFullScreenIntent(createPendingIntent(), true)
            }

            notificationManager.notify(alarma.idModbus, builder.build())
        }

        // Notificación resumen (opcional)
        val summary = NotificationCompat.Builder(context, "alarmas_channel")
            .setContentTitle("${alarmas.size} alarmas activas")
            .setContentText("Toque para ver detalles")
            .setSmallIcon(R.mipmap.ic_notification)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setContentIntent(createPendingIntent())
            .build()

        notificationManager.notify(0, summary)
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, FaultsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun convertPriority(prioridad: String): Int {
        return when (prioridad.lowercase()) {
            "alta" -> NotificationCompat.PRIORITY_HIGH
            "media" -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }
    }
}