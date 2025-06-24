package com.example.asb.faults

data class Alarma(
    val id: Int,           // Cambiado de String a Int
    val codigo: String,    // Nuevo campo
    val mensaje: String,   // Reemplaza a registro/estructura
    val fecha: String,     // Mantenido (formato diferente)
    val prioridad: String  // Nuevo campo
)

//esta clase para parsear el JSON completo
data class AlarmasResponse(
    val status: String,
    val count: Int,
    val timestamp: String,
    val alarmas: List<Alarma>
)