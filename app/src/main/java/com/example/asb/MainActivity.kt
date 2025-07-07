package com.example.asb

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.asb.auth.LoginActivity
import com.example.asb.databinding.ActivityMainBinding
import com.example.asb.db.DataActivity
import com.example.asb.faults.FaultsActivity
import com.example.asb.monitoring.MonitoringActivity
import com.example.asb.about.AboutActivity
import com.example.asb.network.model.ProjectResponse
import com.example.asb.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import com.example.asb.mqtt.MqttProductionForegroundService

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val mainScope = CoroutineScope(Dispatchers.Main)

    companion object { //Para notificacioones
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressBar.visibility = View.VISIBLE
        mainScope.launch {
            val project = withContext(Dispatchers.IO) { loadProjectData() }
            if (project != null) {
                setupUI("", project)
                setupButtons(project)
            } else {
                Toast.makeText(this@MainActivity, "Error: Datos del proyecto incompletos", Toast.LENGTH_SHORT).show()
                finish()
            }
            setupNavigationDrawer()
            setupBackPressHandler()
            binding.progressBar.visibility = View.GONE
        }
        //inicia el segundo plano
        startMqttForegroundService()
        //inicia las notificaciones
        checkAndRequestNotificationPermission()
    }
    //para las notificaciones
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
                Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
            }
            // No necesitas manejar el caso de denegación explícitamente
        }
    }

    private suspend fun loadProjectData(): ProjectResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val workOrderId = intent.getStringExtra("WORK_ORDER") ?: ""
                val projectName = intent.getStringExtra("PROJECT_NAME") ?: ""
                val clientId = intent.getStringExtra("CLIENT_ID") ?: "client_default" // Valor por defecto

                if (workOrderId.isNotEmpty() && projectName.isNotEmpty()) {
                    ProjectResponse(
                        id = workOrderId.toIntOrNull() ?: 0,
                        name = projectName,
                        tipoEquipo = "default",
                        workOrders = listOf(workOrderId),
                        clientId = clientId
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun setupNavigationDrawer() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_about -> {
                    startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }

        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupUI(username: String, project: ProjectResponse) {
        mainScope.launch {
            binding.ivGeneralEquipment.setImageResource(
                when(project.tipoEquipo.lowercase()) {
                    "svv" -> R.mipmap.svv_general
                    "pozo" -> R.mipmap.pozo_general
                    "hidro" -> R.mipmap.hidro_general
                    "carcamo" -> R.mipmap.carcamo_general
                    else -> R.mipmap.default_general
                }
            )
            binding.tvClientName.text = getString(R.string.client_label, username)
            binding.tvProjectType.text = project.name
            binding.tvWorkOrder.text = project.workOrders.firstOrNull() ?: "N/A"
        }
    }
    //para iniciar las notificaciones
    private fun startMqttForegroundService() {
        stopService(Intent(this, MqttProductionForegroundService::class.java))
        val intent = Intent(this, MqttProductionForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun setupButtons(project: ProjectResponse) {
        val clientId = intent.getStringExtra("CLIENT_ID") ?: "client_default"

        binding.btnMonitoring.setOnClickListener {
            startActivity(Intent(this, MonitoringActivity::class.java).apply {
                putExtra("CLIENT_ID", clientId)
                putExtra("WORK_ORDER", project.workOrders.firstOrNull() ?: "project_default")
                putExtra("EQUIPMENT_TYPE", project.tipoEquipo)
            })
        }

        binding.btnFaults.setOnClickListener {
            startActivity(Intent(this, FaultsActivity::class.java).apply {
                // Pasa los parámetros esenciales (como en MonitoringActivity)
                putExtra("CLIENT_ID", clientId) // clientId debe estar definido en MainActivity
                putExtra("WORK_ORDER", project.workOrders.firstOrNull() ?: "project_default")
            })
        }

        binding.btnData.setOnClickListener {
            startActivity(Intent(this, DataActivity::class.java).apply {
                putExtra("CLIENT_ID", clientId)
                putExtra("WORK_ORDER", project.workOrders.firstOrNull())
            })
        }
    }
    //permisos para Android 13+ para notificaciones
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permiso ya concedido, no hacer nada
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Mostrar explicación (opcional)
                    Toast.makeText(this,
                        "Las notificaciones son necesarias para alertas de fallas",
                        Toast.LENGTH_LONG).show()

                    // Solicitar permiso después de explicar
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }

                else -> {
                    // Solicitar permiso directamente
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
        // Para versiones anteriores a Android 13 no se necesita permiso explícito
    }

    private fun logout() {
        mainScope.launch {
            // Detener el servicio
            stopService(Intent(this@MainActivity, MqttProductionForegroundService::class.java))

            // Limpia las credenciales guardadas
            SessionManager.clearSession(this@MainActivity)

            // Redirige al Login y limpia el stack de actividades
            startActivity(
                Intent(this@MainActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
        }
    }
}