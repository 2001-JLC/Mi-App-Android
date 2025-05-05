package com.example.asb.network

import com.example.asb.network.model.LoginRequest
import com.example.asb.network.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

// Interfaz que define los endpoints de la API
interface ApiService {
    // Endpoint POST para login que recibe un cuerpo LoginRequest y devuelve LoginResponse
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Endpoint GET para obtener órdenes de trabajo que requiere token de autorización y ID de cliente
    @GET("api/proyect/listProyects")
    suspend fun getWorkOrders(
        @Header("Authorization") token: String,
        @Query("id") clientId: String
    ): Response<WorkOrderResponse>
}

// Clase que representa la respuesta de las órdenes de trabajo
data class WorkOrderResponse(
    @SerializedName("data") val data: List<WorkOrder> // Mapea el campo "data" del JSON a una lista
)

// Clase que representa una orden de trabajo individual
@Parcelize // Permite parcelación para pasar entre actividades
data class WorkOrder(
    @SerializedName("Id") val id: Int, // Mapea campo "Id" del JSON
    @SerializedName("Nombre_Obra") val name: String // Mapea campo "Nombre_Obra" del JSON
) : Parcelable