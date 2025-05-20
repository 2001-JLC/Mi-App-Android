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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verificar sesión expirada incluso si no viene de logout
        if (SessionManager.getToken(this) != null && !SessionManager.isTokenValid(this)) {
            SessionManager.clearSession(this)
            Toast.makeText(this, "Sesión expirada", Toast.LENGTH_SHORT).show()
        }

        // Si viene de un logout, limpia cualquier dato residual
        if (intent?.getBooleanExtra("FROM_LOGOUT", false) == true) {
            SessionManager.clearSession(this)
        }

        setupLoginButton()
    }

    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingrese usuario y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            attemptLogin(username, password)
        }
    }

    private fun attemptLogin(username: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.login(
                    LoginRequest(
                        userName = username,
                        pass = password
                    )
                )

                withContext(Dispatchers.Main) {
                    when {
                        !response.isSuccessful -> {
                            val errorMsg = response.errorBody()?.string() ?: "Código ${response.code()}"
                            showToast("Error: $errorMsg")
                        }
                        response.body() == null -> {
                            showToast("Error: Respuesta vacía del servidor")
                        }
                        response.body()?.message != "Login exitoso" -> {
                            showToast(response.body()?.message ?: "Credenciales incorrectas")
                        }
                        else -> {  // <- CASO DE ÉXITO QUE FALTABA
                            response.body()?.let { loginData ->
                                // Guardar sesión
                                SessionManager.saveSession(
                                    context = this@LoginActivity,
                                    username = username,
                                    token = loginData.token,
                                    clientId = loginData.idCliente.toString()
                                )
                                // Redirigir a la siguiente pantalla
                                startActivity(
                                    Intent(this@LoginActivity, SelectWorkOrderActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        putExtra("TOKEN", loginData.token)
                                        putExtra("ID_CLIENTE", loginData.idCliente.toString())
                                    }
                                )
                                finish()
                            }?: showToast("Error: Datos de sesión inválidos") // Manejo por si loginData es null
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error de conexión: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
}