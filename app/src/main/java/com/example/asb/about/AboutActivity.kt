package com.example.asb.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.asb.R
import com.github.mikephil.charting.BuildConfig

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // Versión
        findViewById<TextView>(R.id.tvVersion).text = "Versión ${BuildConfig.VERSION_NAME}"

        // Botón back
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Web
        findViewById<LinearLayout>(R.id.btnWeb).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.asbombeo.com")))
        }

        // Teléfono
        findViewById<LinearLayout>(R.id.btnPhone).setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+527341089680")))
        }

        // Email
        findViewById<LinearLayout>(R.id.btnEmail).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:contacto@asbombeo.com")
            }
            startActivity(intent)
        }
    }
}