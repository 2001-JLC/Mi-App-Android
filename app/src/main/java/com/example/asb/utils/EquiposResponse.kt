package com.example.asb.utils

import com.example.asb.models.DynamicEquipment
import com.google.gson.annotations.SerializedName

data class EquiposResponse(
    @SerializedName("presion") val presion: Double?,  // Null si no es SVV o no aplica
    @SerializedName("equipos") val equipos: List<DynamicEquipment>  // Lista dinámica
)