package com.example.asb.db.json

import com.google.gson.annotations.SerializedName

data class ElectricDataResponse(
    val equipo1: EquipmentData,
    val equipo2: EquipmentData,
    val timestamp: String,
    val status: String,
    @SerializedName("size_equipo1")
    val sizeEquipo1: Int,
    @SerializedName("size_equipo2")
    val sizeEquipo2: Int
) {
    data class EquipmentData(
        @SerializedName("total_registros")
        val totalRegistros: Int,
        val registros: List<Registro>
    )

    data class Registro(
        val id: Int,
        val timestamp: String,
        val frecuencia: Double,
        val voltaje: Double,
        val corriente: Double,
        val presion: Double,
        val estado: String
    )
}