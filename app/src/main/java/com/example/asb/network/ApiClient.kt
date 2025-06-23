package com.example.asb.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {
    // URLs de respaldo (primaria y secundaria)
    private val BASE_URLS = listOf(
        "http://asbombeo.ddns.net:3000/",  // URL primaria
        "http://192.168.2.68:3000/"        // URL de respaldo local
    )

    // Configuración personalizada del cliente HTTP
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // Timeout para conexión
        .readTimeout(30, TimeUnit.SECONDS)     // Timeout para lectura
        // Interceptor principal para manejo de URLs alternativas
        .addInterceptor { chain ->
            val request = chain.request()
            var response: okhttp3.Response? = null
            var exception: IOException? = null

            // Intentar con cada URL en orden de prioridad
            for (baseUrl in BASE_URLS) {
                try {
                    // Construir nueva URL
                    val newUrl = request.url.toString().replace(BASE_URLS[0], baseUrl)
                    val newRequest = request.newBuilder()
                        .url(newUrl)
                        .header("Connection", "close")  // Cerrar conexión después del request
                        .build()

                    // Ejecutar petición
                    response = chain.proceed(newRequest)

                    // Aceptar respuesta si es exitosa o es error 401 (evita leer body innecesariamente)
                    if (response.isSuccessful || response.code == 401) {
                        break
                    } else {
                        response.close()  // Cerrar respuesta no exitosa
                    }
                } catch (e: IOException) {
                    exception = e  // Guardar excepción para reintentar
                }
            }

            // Retornar última respuesta válida o lanzar excepción
            response ?: throw exception ?: IOException("Todas las URLs fallaron")
        }
        // Interceptor para logging (solo headers para evitar problemas)
        .addNetworkInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .build()

    // Instancia del servicio API
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URLS[0])  // URL base para rutas relativas
            .client(okHttpClient)   // Cliente configurado
            .addConverterFactory(GsonConverterFactory.create())  // Conversor JSON
            .build()
            .create(ApiService::class.java)
    }
}