package com.example.asb.faults

import com.google.gson.annotations.SerializedName

data class Alarma(
    @SerializedName("id_modbus")
    val idModbus: Int,
    val mensaje: String,
    val fecha: String  // Formato ISO 8601: "2025-07-02T18:57:08.351Z"
)

//esta clase para parsear el JSON completo
data class AlarmasResponse(
    val cliente: String,
    val equipo: String,
    val data: AlarmasData
)

data class AlarmasData(
    val alarmas: List<Alarma>,
    val total: Int,
    @SerializedName("last_update")
    val lastUpdate: String
)