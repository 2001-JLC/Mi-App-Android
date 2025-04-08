package com.example.asb.models

import com.google.gson.annotations.SerializedName


data class DynamicEquipment(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("datos") val datos: Map<String, Any>,
    @SerializedName("tipo") var tipo: String
)