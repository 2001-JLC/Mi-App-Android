package com.example.asb.network.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Clase que representa la respuesta de proyectos (parcelable)
@Parcelize
data class ProjectResponse(
    val id: Int, // ID del proyecto
    val name: String, // Nombre del proyecto
    val tipoEquipo: String, // Tipo de equipo
    val workOrders: List<String> // Lista de órdenes de trabajo
) : Parcelable