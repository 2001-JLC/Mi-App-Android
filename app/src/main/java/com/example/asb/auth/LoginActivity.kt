package com.example.asb.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.asb.MainActivity
import com.example.asb.databinding.ActivityLoginBinding
import com.example.asb.db.DatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dbHelper = DatabaseHelper(applicationContext)
        setupLoginButton()
    }

    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val workOrder = binding.etWorkOrder.text.toString().trim().padStart(4, '0')

            if (username.isEmpty() || password.isEmpty() || workOrder.isEmpty()) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            attemptLogin(username, password, workOrder)
        }
    }

    private fun attemptLogin(username: String, password: String, workOrder: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!dbHelper.validateUser(username, password)) {
                    showToast("Usuario/contraseña incorrectos")
                    return@launch
                }

                val userId = dbHelper.getUserId(username).toString().padStart(3, '0')
                val projectInfo = dbHelper.getProjectInfo(userId, workOrder)

                if (projectInfo == null) {
                    showToast("Orden de trabajo no válida para este usuario")
                    return@launch
                }

                val (projectType, projectName) = projectInfo
                launchMainActivity(username, userId, workOrder, projectType, projectName)

            } catch (e: Exception) {
                showToast("Error: ${e.message}")
                Log.e("LOGIN_ERROR", "Error en login", e)
            }
        }
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun launchMainActivity(
        username: String,
        userId: String,
        workOrder: String,
        projectType: String,
        projectName: String
    ) {
        withContext(Dispatchers.Main) {
            startActivity(
                Intent(this@LoginActivity, MainActivity::class.java).apply {
                    putExtra("MQTT_TOPIC_BASE", "$userId/$workOrder/$projectType/02")
                    putExtra("USERNAME", username)
                    putExtra("WORK_ORDER", workOrder)
                    putExtra("PROJECT_TYPE", projectType)
                    putExtra("PROJECT_NAME", projectName)
                }
            )
            finish()
        }
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }
}