package com.example.asb.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Objeto singleton que gestiona el almacenamiento seguro de la sesión del usuario
object SessionManager {
    // Nombres de las claves para SharedPreferences
    private const val PREFS_NAME = "user_session"         // Nombre del archivo de preferencias
    private const val KEY_USERNAME = "username"          // Clave para el nombre de usuario
    private const val KEY_TOKEN = "token"                // Clave para el token de autenticación
    private const val KEY_CLIENT_ID = "client_id"        // Clave para el ID de cliente
    private const val KEY_USE_BIOMETRIC = "use_biometric" // Clave para la preferencia de huella
    private const val KEY_TOKEN_EXPIRATION = "token_expiration"
    private const val DEFAULT_TOKEN_EXPIRATION_SECONDS = 3600L


    // Obtiene la instancia de SharedPreferences
    private fun getSecurePrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Guarda los datos de sesión del usuario
     * @param context Contexto de la aplicación
     * @param username Nombre de usuario
     * @param token Token de autenticación
     * @param useBiometric Si el usuario quiere usar autenticación biométrica
     * @param clientId ID del cliente (opcional)
     */
    fun saveSession(
        context: Context,
        username: String,
        token: String,
        useBiometric: Boolean,
        clientId: String? = null  // Parámetro opcional para el ID de cliente
    ) {
        val expirationTime = System.currentTimeMillis() + (DEFAULT_TOKEN_EXPIRATION_SECONDS * 1000)

        getSecurePrefs(context).edit().apply {  // Inicia edición de preferencias
            putString(KEY_USERNAME, username)  // Guarda nombre de usuario
            putString(KEY_TOKEN, token)        // Guarda token
            putLong(KEY_TOKEN_EXPIRATION, expirationTime) // Guardamos tiempo fijo
            putBoolean(KEY_USE_BIOMETRIC, useBiometric)  // Guarda preferencia de huella
            clientId?.let { putString(KEY_CLIENT_ID, it) }  // Guarda clientId si no es null
            apply()  // Aplica los cambios (async)
        }
    }

    /**
     * Verifica si el token actual es válido (no ha expirado)
     */
    fun isTokenValid(context: Context): Boolean { // <-- Añade este método crucial
        val expirationTime = getSecurePrefs(context).getLong(KEY_TOKEN_EXPIRATION, 0)
        return System.currentTimeMillis() < expirationTime
    }

    /**
     * Versión segura para obtener token (verifica expiración)
     */
    fun getValidToken(context: Context): String? { // <-- Método recomendado
        return if (isTokenValid(context)) {
            getToken(context)
        } else {
            null
        }
    }

    /**
     * Obtiene el ID de cliente guardado
     * @return ID del cliente o null si no existe
     */
    fun getClientId(context: Context): String? {
        return getSecurePrefs(context).getString(KEY_CLIENT_ID, null)
    }

    /**
     * Obtiene el nombre de usuario guardado
     * @return Nombre de usuario o null si no existe
     */
    fun getUsername(context: Context): String? {
        return getSecurePrefs(context).getString(KEY_USERNAME, null)
    }

    /**
     * Obtiene el token de autenticación
     * @return Token guardado o null si no existe
     */
    fun getToken(context: Context): String? {
        return getSecurePrefs(context).getString(KEY_TOKEN, null)
    }

    /**
     * Verifica si el usuario activó la autenticación biométrica
     * @return true si está activada, false si no
     */
    fun shouldUseBiometric(context: Context): Boolean {
        return getSecurePrefs(context).getBoolean(KEY_USE_BIOMETRIC, false)
    }

    /**
     * Limpia TODOS los datos de sesión (para logout)
     */
    fun clearSession(context: Context) {
        getSecurePrefs(context).edit().apply {
            remove(KEY_USERNAME)      // Elimina usuario
            remove(KEY_TOKEN)        // Elimina token
            remove(KEY_USE_BIOMETRIC) // Elimina preferencia de huella
            remove(KEY_CLIENT_ID)     // Elimina ID de cliente
            apply()  // Aplica los cambios
        }
    }
}