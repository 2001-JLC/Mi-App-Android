package com.example.asb.faults.chart

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.asb.R
import com.github.mikephil.charting.charts.BarChart


class ChartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

        val chartData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("chartData", StackedBarEntry::class.java) ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("chartData") ?: emptyList()
        }

        val barChart = findViewById<BarChart>(R.id.barChart)
        if (barChart == null) {
            Log.e("ChartActivity", "BarChart no encontrado en el layout")
            finish()
            return
        }

        ChartHelper().setupChart(barChart, chartData)
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }
    }
}