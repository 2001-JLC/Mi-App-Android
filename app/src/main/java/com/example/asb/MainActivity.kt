package com.example.asb

import android.content.Intent
import android.os.Bundle
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

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mqttTopicBase = intent.getStringExtra("MQTT_TOPIC_BASE") ?: "001/0001/02/02"
        val username = intent.getStringExtra("USERNAME") ?: ""
        val workOrder = intent.getStringExtra("WORK_ORDER") ?: ""
        val projectName = intent.getStringExtra("PROJECT_NAME") ?: getDefaultProjectName(mqttTopicBase)

        setupUI(mqttTopicBase, username, workOrder, projectName)
        setupButtons(mqttTopicBase)
        setupNavigationDrawer()
        setupBackPressHandler()
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
                    // Reemplaza showAboutDialog() por esto:
                    startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
                    logout()
                    drawerLayout.closeDrawer(GravityCompat.START)
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

    private fun setupUI(mqttTopic: String, username: String, workOrder: String, projectName: String) {
        binding.ivGeneralEquipment.setImageResource(
            when(mqttTopic.split('/')[2]) {
                "01" -> R.mipmap.svv_general
                "02" -> R.mipmap.pozo_general
                "03" -> R.mipmap.hidro_general
                "04" -> R.mipmap.carcamo_general
                else -> R.mipmap.default_general
            }
        )
        binding.tvClientName.text = getString(R.string.client_label, username)
        binding.tvWorkOrder.text = getString(R.string.order_label, workOrder)
        binding.tvProjectType.text = getString(R.string.project_label, projectName)
    }

    private fun getDefaultProjectName(mqttTopic: String): String {
        return when(mqttTopic.split('/')[2]) {
            "01" -> "SVV"
            "02" -> "Pozo de agua"
            "03" -> "Sistema hidroneumático"
            "04" -> "Cárcamo de bombeo"
            else -> "Proyecto desconocido"
        }
    }

    private fun setupButtons(mqttTopicBase: String) {
        binding.btnMonitoring.setOnClickListener {
            startActivity(Intent(this, MonitoringActivity::class.java).apply {
                putExtra("MQTT_TOPIC_DATA", "$mqttTopicBase/Datos")
                putExtra("EQUIPMENT_TYPE", mqttTopicBase.split('/')[2])
            })
        }

        binding.btnFaults.setOnClickListener {
            startActivity(Intent(this, FaultsActivity::class.java))
        }

        binding.btnData.setOnClickListener {
            startActivity(Intent(this, DataActivity::class.java))
        }

        binding.btnBitacora.setOnClickListener {
            startActivity(Intent(this, BitacoraActivity::class.java))
        }
    }


    private fun logout() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}