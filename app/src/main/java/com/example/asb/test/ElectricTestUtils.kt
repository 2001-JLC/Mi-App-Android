package com.example.asb.test

import android.content.Context
import android.util.Log
import com.example.asb.db.electricaldatalogtest.ElectricData
import com.example.asb.db.electricaldatalogtest.ElectricDataDatabase

object ElectricTestUtils {
    // Umbrales (ajústalos según tus necesidades)
    private const val MIN_VOLTAGE = 200.0   // Voltaje mínimo normal
    private const val MAX_VOLTAGE = 240.0   // Voltaje máximo normal
    private const val MIN_PRESSURE = 2.4    // Presión mínima normal
    private const val MAX_PRESSURE = 3.2    // Presión máxima normal

    suspend fun checkAndLogVoltage(
        context: Context,
        voltage: Double,
        equipmentName: String,
        currentStatus: String
    ): Boolean {
        return if (shouldCheckValues(currentStatus, voltage)) { // <- Nueva validación
            if (voltage < MIN_VOLTAGE || voltage > MAX_VOLTAGE) {
                logAnomaly(
                    context = context,
                    type = if (voltage < MIN_VOLTAGE) "VOLTAGE_LOW" else "VOLTAGE_HIGH",
                    value = voltage,
                    equipmentName = equipmentName
                )
                true
            } else false
        } else false
    }

    // Método nuevo para validaciones comunes
    private fun shouldCheckValues(status: String, value: Double): Boolean {
        val estadosInvalidos = listOf("APAGADO", "DESCONOCIDO", "INICIANDO")
        return !estadosInvalidos.contains(status.uppercase()) && value > 0.0
    }


    suspend fun checkAndLogPressure(
        context: Context,
        pressure: Double,
        equipmentName: String,
        currentStatus: String
    ): Boolean {
        return if (currentStatus == "ENCENDIDO" && (pressure < MIN_PRESSURE || pressure > MAX_PRESSURE)) {
            logAnomaly(
                context = context,
                type = if (pressure < MIN_PRESSURE) "PRESSURE_LOW" else "PRESSURE_HIGH",
                value = pressure,
                equipmentName = equipmentName
            )
            true
        } else false
    }

    private suspend fun logAnomaly(
        context: Context,
        type: String,
        value: Double,
        equipmentName: String
    ) {
        try {
            ElectricDataDatabase.getDatabase(context).electricDataDao().insert(
                ElectricData(
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    value = value,
                    equipmentName = equipmentName
                )
            )
            Log.w("ELECTRIC_TEST", "⚠️ [$equipmentName] Anomalía registrada: $type = $value")
        } catch (e: Exception) {
            Log.e("ELECTRIC_TEST", "Error al guardar $type", e)
        }
    }
}