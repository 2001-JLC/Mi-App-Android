// JsonParser.kt
package com.example.asb.utils

import android.util.Log
import com.google.gson.Gson

class JsonParser {
    private val gson = Gson()

    fun parseCombinedData(jsonString: String): EquiposResponse? {
        return try {
            gson.fromJson(jsonString, EquiposResponse::class.java).also {
                Log.d("JSON_PARSE", "Datos parseados: Presión=${it.presion}, Equipos=${it.equipos.size}")
            }
        } catch (e: Exception) {
            Log.e("JSON_ERROR", "Error al parsear JSON: ${e.message}")
            null
        }
    }
}