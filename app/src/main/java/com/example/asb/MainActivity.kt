package com.example.asb

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.asb.auth.LoginActivity
import com.example.asb.binnacle.BitacoraActivity
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

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val mainScope = CoroutineScope(Dispatchers.Main)

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

    private fun setupButtons(project: ProjectResponse) {
        val clientId = intent.getStringExtra("CLIENT_ID") ?: "client_default"

        // Configuración simplificada con extension function
        fun Intent.putProjectExtras() = apply {
            putExtra("PROJECT_ID", project.id.toString())
            putExtra("EQUIPMENT_TYPE", project.tipoEquipo)
            putExtra("CLIENT_ID", clientId)
        }

        binding.btnMonitoring.setOnClickListener {
           startActivity(Intent(this, MonitoringActivity::class.java).putProjectExtras())
        }

        binding.btnFaults.setOnClickListener {
            startActivity(Intent(this, FaultsActivity::class.java).putProjectExtras())
        }

        binding.btnData.setOnClickListener {
            startActivity(Intent(this, DataActivity::class.java).apply {
                putExtra("WORK_ORDER", project.workOrders.firstOrNull())
            })
        }

        binding.btnBitacora.setOnClickListener {
            startActivity(Intent(this, BitacoraActivity::class.java).apply {
                putExtra("WORK_ORDER", project.workOrders.firstOrNull())
            })
        }
    }

    private fun logout() {
        mainScope.launch {
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