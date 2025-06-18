package com.example.asb.faults.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.asb.R
import com.example.asb.faults.FaultsActivity
import kotlin.random.Random

//estaclase interactua con MqttForegroundService.kt
class MqttNotificationManager(private val context: Context) {
    // Variables para almacenar temporalmente los datos de la alarma
    private var registro: String? = null
    private var mensaje: String? = null
    private var fecha: String? = null

    // Setters que actualizan los datos y verifican si se puede mostrar la notificación
    fun setRegistro(value: String) { registro = value; tryShowNotification() }
    fun setMensaje(value: String) { mensaje = value; tryShowNotification() }
    fun setFecha(value: String) { fecha = value; tryShowNotification() }

    // Intenta mostrar la notificación solo si todos los datos están disponibles
    private fun tryShowNotification() {
        if (registro != null && mensaje != null && fecha != null) {
            showNotification()
            registro = null; mensaje = null; fecha = null // Resetea los datos después de mostrar
        }
    }

    // Construye y muestra la notificación de alarma
    private fun showNotification() {
        // Verifica el permiso para Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return // Si no hay permiso, no muestra la notificación
        }

        // Intent para abrir FaultsActivity al tocar la notificación
        val intent = Intent(context, FaultsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        // PendingIntent para la acción al tocar la notificación
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
            .setContentTitle(mensaje)
            .setContentText("$registro - $fecha")
            .setSmallIcon(R.mipmap.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Se cierra al tocar
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Muestra la notificación con un ID aleatorio (para múltiples notificaciones)
        NotificationManagerCompat.from(context).notify(Random.nextInt(), notification)
    }
}