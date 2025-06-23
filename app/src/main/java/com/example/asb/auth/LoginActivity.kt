package com.example.asb.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.asb.databinding.ActivityLoginBinding
import com.example.asb.network.ApiClient
import com.example.asb.network.model.LoginRequest
import com.example.asb.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.IOException

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verificación de sesión expirada al iniciar la actividad
        if (SessionManager.getToken(this) != null && !SessionManager.isTokenValid(this)) {
            SessionManager.clearSession(this)
            Toast.makeText(this, "Sesión expirada", Toast.LENGTH_SHORT).show()
        }

        // Limpieza de datos si viene de logout
        if (intent?.getBooleanExtra("FROM_LOGOUT", false) == true) {
            SessionManager.clearSession(this)
        }

        setupLoginButton()
    }

    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Validación básica de campos
            when {
                username.isEmpty() -> binding.etUsername.error = "Ingresa tu usuario"
                password.isEmpty() -> binding.etPassword.error = "Ingresa tu contraseña"
                else -> attemptLogin(username, password) // Solo proceder si los campos no están vacíos
            }
        }
    }

    private fun attemptLogin(username: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE // Mostrar indicador de carga

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Realizar petición de login
                val response = ApiClient.apiService.login(
                    LoginRequest(
                        userName = username,
                        pass = password
                    )
                )

                withContext(Dispatchers.Main) {
                    // Manejar respuesta del servidor
                    when {
                        !response.isSuccessful -> {
                            // Manejo de errores HTTP (401, 500, etc.)
                            val errorMsg = when (response.code()) {
                                401 -> "Usuario o contraseña incorrectos"
                                500 -> "Error en el servidor. Intenta más tarde"
                                else -> "Error al iniciar sesión (Código ${response.code()})"
                            }
                            showToast(errorMsg)
                        }
                        response.body() == null -> {
                            // Respuesta vacía del servidor
                            showToast("Error: No se recibieron datos del servidor")
                        }
                        response.body()?.message != "Login exitoso" -> {
                            // Mensaje personalizado del servidor
                            showToast(response.body()?.message ?: "Credenciales incorrectas")
                        }
                        else -> {
                            // Login exitoso - Guardar sesión y redirigir
                            response.body()?.let { loginData ->
                                SessionManager.saveSession(
                                    context = this@LoginActivity,
                                    username = username,
                                    token = loginData.token,
                                    clientId = loginData.idCliente.toString()
                                )
                                startActivity(
                                    Intent(this@LoginActivity, SelectWorkOrderActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        putExtra("TOKEN", loginData.token)
                                        putExtra("ID_CLIENTE", loginData.idCliente.toString())
                                    }
                                )
                                finish()
                            } ?: showToast("Error: No se pudo guardar la sesión")
                        }
                    }
                }
            } catch (e: Exception) {
                // Manejo de errores de red/excepciones
                withContext(Dispatchers.Main) {
                    showToast(
                        when (e) {
                            is IOException -> "Error de conexión. Verifica tu internet"
                            is TimeoutCancellationException -> "Tiempo de espera agotado"
                            else -> "Error inesperado: ${e.message}"
                        }
                    )
                }
            } finally {
                // Ocultar indicador de carga siempre
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private suspend fun showToast(message: String) {
        // Helper para mostrar toasts en el hilo principal
        withContext(Dispatchers.Main) {
            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
}