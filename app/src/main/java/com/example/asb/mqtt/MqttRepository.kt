package com.example.asb.mqtt

import android.util.Log

class MqttRepository(private val manager: MqttClientManager) {
    fun subscribe(topic: String, callback: MqttCallbackHandler) {
        manager.addCallback(callback)
        manager.subscribe(topic) { success ->
            if (success) Log.d("MQTT_REPO", "Suscrito a $topic")
            else Log.e("MQTT_REPO", "Error al suscribirse a $topic")
        }
    }
}
