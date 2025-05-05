package com.example.asb.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.asb.databinding.ActivityLoginBinding
import com.example.asb.network.ApiClient
import com.example.asb.network.model.LoginRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    // ViewBinding para acceder a las vistas del layout
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Infla el layout usando ViewBinding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura el botón de login al crear la actividad
        setupLoginButton()
    }

    private fun setupLoginButton() {
        // Asigna un click listener al botón de login
        binding.btnLogin.setOnClickListener {
            // Obtiene los valores de los campos de texto
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Valida que los campos no estén vacíos
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingrese usuario y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Sale si hay campos vacíos
            }

            // Intenta hacer login si los campos son válidos
            attemptLogin(username, password)
        }
    }

    private fun attemptLogin(username: String, password: String) {
        // Muestra el progress bar durante la operación
        binding.progressBar.visibility = View.VISIBLE

        // Lanza una corrutina en el hilo de IO (para operaciones de red)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Realiza la llamada a la API de login
                val response = ApiClient.apiService.login(
                    LoginRequest(
                        userName = username,
                        pass = password
                    )
                )

                // Cambia al hilo principal para actualizar la UI
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // Si la respuesta es exitosa, obtiene los datos
                        val loginData = response.body()

                        // Verifica si el mensaje del servidor indica éxito
                        if (loginData?.message == "Login exitoso") {
                            // Redirige a la actividad de selección de órdenes de trabajo
                            startActivity(
                                Intent(this@LoginActivity, SelectWorkOrderActivity::class.java).apply {
                                    // Pasa el token y el ID de cliente como extras
                                    putExtra("TOKEN", loginData.token)
                                    putExtra("ID_CLIENTE", loginData.idCliente.toString())
                                }
                            )
                            // Finaliza esta actividad para que no se pueda volver atrás
                            finish()
                        } else {
                            // Muestra mensaje de error si las credenciales son incorrectas
                            showToast(loginData?.message ?: "Credenciales incorrectas")
                        }
                    } else {
                        // Muestra error de la respuesta si no fue exitosa
                        showToast("Error: ${response.errorBody()?.string() ?: "Código ${response.code()}"}")
                    }
                }
            } catch (e: Exception) {
                // Maneja errores de conexión
                showToast("Error de conexión: ${e.message}")
            } finally {
                // Oculta el progress bar al finalizar (éxito o error)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    // Función de extensión para mostrar Toasts de forma segura
    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
}