package com.example.asb.network.model

import com.google.gson.annotations.SerializedName

// Clase que representa la solicitud de login
data class LoginRequest(
    @SerializedName("userName") val userName: String, // Nombre de usuario
    @SerializedName("pass") val pass: String // Contraseña
)