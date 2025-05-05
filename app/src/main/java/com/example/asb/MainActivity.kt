package com.example.asb

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.asb.auth.LoginActivity
import com.example.asb.binnacle.BitacoraActivity
import com.example.asb.databinding.ActivityMainBinding
import com.example.asb.db.DataActivity
import com.example.asb.faults.FaultsActivity
import com.example.asb.monitoring.MonitoringActivity
import com.google.android.material.navigation.NavigationView
import com.example.asb.about.AboutActivity
import com.example.asb.network.model.ProjectResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private val mainScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mostrar progreso mientras se carga
        binding.progressBar.visibility = View.VISIBLE

        mainScope.launch {
            // Operaciones en segundo plano
            val project = withContext(Dispatchers.IO) {
                loadProjectData()
            }

            // Actualizar UI en el hilo principal
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

                if (workOrderId.isNotEmpty() && projectName.isNotEmpty()) {
                    ProjectResponse(
                        id = workOrderId.toIntOrNull() ?: 0,
                        name = projectName,
                        tipoEquipo = "default",
                        workOrders = listOf(workOrderId)
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
        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)

        binding.btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_about -> {
                    mainScope.launch {
                        startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    true
                }
                R.id.nav_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
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
        binding.btnMonitoring.setOnClickListener {
            mainScope.launch {
                startActivity(Intent(this@MainActivity, MonitoringActivity::class.java).apply {
                    putExtra("PROJECT_ID", project.id.toString())
                    putExtra("EQUIPMENT_TYPE", project.tipoEquipo)
                })
            }
        }

        binding.btnFaults.setOnClickListener {
            mainScope.launch {
                startActivity(Intent(this@MainActivity, FaultsActivity::class.java).apply {
                    putExtra("PROJECT_ID", project.id.toString())
                })
            }
        }

        binding.btnData.setOnClickListener {
            mainScope.launch {
                startActivity(Intent(this@MainActivity, DataActivity::class.java))
            }
        }

        binding.btnBitacora.setOnClickListener {
            mainScope.launch {
                startActivity(Intent(this@MainActivity, BitacoraActivity::class.java))
            }
        }
    }

    private fun logout() {
        mainScope.launch {
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel() // Cancela todas las corrutinas cuando la actividad se destruye
    }
}