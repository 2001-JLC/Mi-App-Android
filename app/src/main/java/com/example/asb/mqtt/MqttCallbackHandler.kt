package com.example.asb.mqtt


interface MqttCallbackHandler {
    fun onMessageReceived(topic: String, message: String)
    fun onConnectionLost(cause: Throwable)
    fun onConnectionSuccess()
}