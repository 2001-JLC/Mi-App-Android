package com.example.asb.auth

import android.app.AlertDialog
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
    private val biometricHelper = BiometricHelper(this)

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

        // Verificar si hay credenciales guardadas al iniciar
        checkSavedCredentials()
        setupLoginButton()
    }

    private fun checkSavedCredentials() {
        val savedUser = SessionManager.getUsername(this)
        if (!savedUser.isNullOrEmpty() && SessionManager.shouldUseBiometric(this)) {
            binding.etUsername.setText(savedUser)
            biometricHelper.showPrompt(
                title = "Iniciar sesión con huella",
                onSuccess = { goToNextScreenWithToken() },
                onError = { error ->
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    // No hacemos nada, el usuario puede ingresar manualmente
                }
            )
        }
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
                        else -> {
                            response.body()?.let { loginData ->
                                handleSuccessfulLogin(username, loginData.token, loginData.idCliente.toString())
                            }
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

    private fun handleSuccessfulLogin(username: String, token: String, clientId: String) {
        // Preguntar si quiere usar huella (solo si no está ya configurado)
        if (!SessionManager.shouldUseBiometric(this)) {
            showBiometricDialog(username, token, clientId)
        } else {
            goToNextScreen(token, clientId)
        }
    }

    private fun showBiometricDialog(username: String, token: String, clientId: String) {
        AlertDialog.Builder(this)
            .setTitle("¿Usar huella en próximos inicios?")
            .setMessage("Puedes agilizar tu acceso futuro usando tu huella dactilar")
            .setPositiveButton("Sí") { _, _ ->
                SessionManager.saveSession(
                    context = this,
                    username = username,
                    token = token,
                    useBiometric = true,
                    clientId = clientId
                )
                goToNextScreen(token, clientId)
            }
            .setNegativeButton("No") { _, _ ->
                SessionManager.saveSession(
                    context = this,
                    username = username,
                    token = token,
                    useBiometric = false,
                    clientId = clientId
                )
                goToNextScreen(token, clientId)
            }
            .setCancelable(false)
            .show()
    }

    private fun goToNextScreen(token: String, clientId: String) {
        startActivity(
            Intent(this, SelectWorkOrderActivity::class.java).apply {
                putExtra("TOKEN", token)
                putExtra("ID_CLIENTE", clientId)
            }
        )
        finish()
    }

    // Actualiza este método para verificar token válido
    private fun goToNextScreenWithToken() {
        SessionManager.getValidToken(this)?.let { token ->
            goToNextScreen(token, SessionManager.getClientId(this) ?: "")
        }
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
}