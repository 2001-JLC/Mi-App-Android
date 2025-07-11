package com.example.asb.topic

object MqttTopicManager {
    private const val BASE_TOPIC = "asb/telemetria"

    // Para SelectWorkOrder -> MainActivity (Operaciones/Bombas)
    fun getOperationsTopic(clientId: String, projectId: String): String {
        return "$BASE_TOPIC/client$clientId/proyect$projectId/operaciones/bombas/data"
    }

    // Para MonitoringActivity
    fun getMonitoringTopic(clientId: String, projectId: String): String {
        return "$BASE_TOPIC/client$clientId/proyect$projectId/monitoreo/DataPressure"
    }

    // Para Alarmas (MqttForegroundService - futuro)
    fun getAlarmsNotificationTopic(clientId: String, projectId: String): String {
        return "$BASE_TOPIC/client$clientId/proyect$projectId/alarmas/notification"
    }

    // Alarmas - Solicitud (FaultsActivity)
    fun getAlarmsRequestTopic(clientId: String, projectId: String): String {
        return "$BASE_TOPIC/client$clientId/proyect$projectId/alarmas/getData"
    }

    // Alarmas - Respuesta (FaultsActivity)
    fun getAlarmsResponseTopic(clientId: String, projectId: String): String {
        return "$BASE_TOPIC/client$clientId/proyect$projectId/alarmas/data"
    }
    // Para DataActivity - Petición
    fun getDataRequestTopic(clientId: String, projectId: String): String {
        return "$BASE_TOPIC/client$clientId/proyect$projectId/DataElec/getData"
    }

    // Para DataActivity - Recepción
    fun getDataResponseTopic(clientId: String, projectId: String): String {
        return "$BASE_TOPIC/client$clientId/proyect$projectId/DataElec/Data"
    }
}