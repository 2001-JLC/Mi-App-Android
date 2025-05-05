package com.example.asb.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Objeto singleton que proporciona una única instancia de Retrofit para toda la app
object ApiClient {
    // URL base del servidor API
    private const val BASE_URL = "http://192.168.2.68:3000/"

    // Configuración del cliente HTTP con OkHttp
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // Tiempo máximo para establecer conexión
        .readTimeout(30, TimeUnit.SECONDS) // Tiempo máximo para leer respuesta
        .retryOnConnectionFailure(true)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // ← Esto mostrará requests/responses
        })
        .addInterceptor { chain ->
            // Interceptor que añade cabecera "Connection: close" a cada petición
            val request = chain.request().newBuilder()
                .header("Connection", "close")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    // Instancia lazy del servicio API que se crea solo cuando se necesita por primera vez
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // Establece URL base
            .client(okHttpClient) // Asigna el cliente HTTP configurado
            .addConverterFactory(GsonConverterFactory.create()) // Añade conversor JSON
            .build()
            .create(ApiService::class.java) // Crea implementación de la interfaz ApiService
    }
}