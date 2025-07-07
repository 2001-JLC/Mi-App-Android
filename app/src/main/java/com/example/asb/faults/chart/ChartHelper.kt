package com.example.asb.faults.chart

import android.graphics.Color
import android.util.Log
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ChartHelper {
    // Data classes actualizadas para el nuevo JSON
    private data class JsonChartData(
        val cliente: String,
        val equipo: String,
        val data: AlarmasData
    )

    private data class AlarmasData(
        val alarmas: List<AlarmaChart>,
        val total: Int,
        @SerializedName("last_update")
        val lastUpdate: String
    )

    private data class AlarmaChart(
        @SerializedName("id_modbus")
        val idModbus: Int,
        val mensaje: String,
        val fecha: String  // Formato ISO 8601: "2025-07-02T18:57:08.351Z"
    )

    fun getChartData(rawData: String): List<StackedBarEntry> {
        return try {
            val jsonData = Gson().fromJson(rawData, JsonChartData::class.java)
            val monthFormat = SimpleDateFormat("MMM yyyy", Locale.US)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            jsonData.data.alarmas.mapNotNull { alarm ->
                try {
                    val date = dateFormat.parse(alarm.fecha) ?: return@mapNotNull null
                    val monthYear = monthFormat.format(date)

                    StackedBarEntry(
                        timeLabel = monthYear,
                        bomba1Count = if (alarm.mensaje.contains("BOMBA 1", ignoreCase = true)) 1 else 0,
                        bomba2Count = if (alarm.mensaje.contains("BOMBA 2", ignoreCase = true)) 1 else 0,
                        sistemaCount = if (alarm.idModbus in 120..129) 1 else 0 // Ejemplo: IDs 120-129 son del sistema
                    )
                } catch (e: Exception) {
                    Log.e("ChartHelper", "Error parsing alarm: ${alarm.idModbus}", e)
                    null
                }
            }.groupBy { it.timeLabel }
                .map { (month, entries) ->
                    StackedBarEntry(
                        month,
                        entries.sumOf { it.bomba1Count },
                        entries.sumOf { it.bomba2Count },
                        entries.sumOf { it.sistemaCount }
                    )
                }.sortedBy {
                    SimpleDateFormat("MMM yyyy", Locale.US).parse(it.timeLabel)?.time ?: 0
                }
        } catch (e: Exception) {
            Log.e("ChartHelper", "Error parsing JSON", e)
            emptyList()
        }
    }

    fun setupChart(barChart: BarChart, data: List<StackedBarEntry>) {
        val entries = data.mapIndexed { idx, entry ->
            BarEntry(idx.toFloat(), floatArrayOf(
                entry.bomba1Count.toFloat(),
                entry.bomba2Count.toFloat(),
                entry.sistemaCount.toFloat()
            ))
        }

        val dataSet = BarDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#FF5722"),  // Naranja
                Color.parseColor("#2196F3"),  // Azul
                Color.parseColor("#4CAF50")   // Verde
            )
            stackLabels = arrayOf("Bomba 1", "Bomba 2", "Sistema")
            valueTextSize = 10f
            valueTextColor = Color.WHITE
        }

        // Corrección clave: Usar barChart.data en lugar de solo 'data'
        barChart.data = BarData(dataSet).apply {
            barWidth = 0.6f
            setValueFormatter(DefaultValueFormatter(0))
        }

        barChart.apply {
            // Configuración de ejes
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(data.map { it.timeLabel })
                granularity = 1f
                labelRotationAngle = 45f
                textSize = 11f
                textColor = Color.WHITE
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                yOffset = 10f
                // Añade estas líneas para hacer visible la línea del eje X:
                setDrawAxisLine(true)  // Habilita la línea del eje
                axisLineColor = Color.WHITE  // Color de la línea (ajusta según tu tema)
                axisLineWidth = 1.5f  // Grosor de la línea
            }

            axisLeft.apply {
                textSize = 11f
                textColor = Color.WHITE
                granularity = 1f
                axisMinimum = 0f
                gridColor = Color.parseColor("#333333")
            }

            // Configuración de leyenda
            legend.apply {
                textSize = 12f
                textColor = Color.WHITE
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                yOffset = 25f
                xOffset = 10f
            }

            setExtraOffsets(15f, 15f, 15f, 40f)
            setTouchEnabled(true)
            setPinchZoom(true)
            setScaleEnabled(true)
            setVisibleXRangeMaximum(6f)
            description.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }
}