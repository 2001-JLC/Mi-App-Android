package com.example.asb.network.model

import com.google.gson.annotations.SerializedName

// Clase que representa la respuesta del login
data class LoginResponse(
    @SerializedName("message") val message: String, // Mensaje del servidor
    @SerializedName("idCliente") val idCliente: Int, // ID del cliente
    @SerializedName("idUsuario") val idUsuario: Int, // ID del usuario
    @SerializedName("perfil") val perfil: String, // Perfil del usuario
    @SerializedName("token") val token: String, // Token de autenticación
    @SerializedName("sessionId") val sessionId: String // ID de sesión
)