package com.example.asb.faults.chart

import android.graphics.Color
import android.util.Log
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Locale

class ChartHelper {
    // Nueva data class para el parseo del JSON
    private data class JsonChartData(
        val status: String,
        val alarmas: List<AlarmaChart>
    )

    private data class AlarmaChart(
        val codigo: String,
        val mensaje: String,
        val fecha: String
    )

    fun getChartData(rawData: String): List<StackedBarEntry> {
        return try {
            val jsonData = Gson().fromJson(rawData, JsonChartData::class.java)
            val monthFormat = SimpleDateFormat("MMM yyyy", Locale.US)
            val dateFormat = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US)

            jsonData.alarmas.mapNotNull { alarm ->
                try {
                    val date = dateFormat.parse(alarm.fecha) ?: return@mapNotNull null
                    val monthYear = monthFormat.format(date)

                    StackedBarEntry(
                        timeLabel = monthYear,
                        bomba1Count = if (alarm.mensaje.contains("BOMBA 1", ignoreCase = true)) 1 else 0,
                        bomba2Count = if (alarm.mensaje.contains("BOMBA 2", ignoreCase = true)) 1 else 0,
                        sistemaCount = if (alarm.codigo.startsWith("120") || alarm.codigo.startsWith("121")) 1 else 0
                    )
                } catch (e: Exception) {
                    Log.e("ChartHelper", "Error parsing alarm: ${alarm.codigo}", e)
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
            colors = listOf(Color.RED, Color.BLUE, Color.GREEN)
            stackLabels = arrayOf("Bomba 1", "Bomba 2", "Sistema")
        }

        barChart.apply {
            this.data = BarData(dataSet)
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(data.map { it.timeLabel })
                granularity = 1f // Evita etiquetas duplicadas
                labelCount = data.size // Muestra todas las etiquetas
                labelRotationAngle = 45f // Rotación para mejor legibilidad
                setAvoidFirstLastClipping(true)  // Evita que se corten las etiquetas
                textColor = Color.WHITE  // Color definido
                textSize = 10f
            }
            // Configurar el Eje Y
            axisLeft.apply {
                granularity = 1f  // Muestra solo valores enteros
                textColor = Color.WHITE //color de los numeros
            }
            axisRight.isEnabled = false  // Deshabilita el eje derecho

            // Configuración de interactividad
            setPinchZoom(true) // Zoom con gestos
            isDragEnabled = true // Scroll horizontal
            setVisibleXRangeMaximum(6f) // Muestra 6 meses a la vez
            description.isEnabled = false // Opcional: oculta la descripción
            isDragEnabled = true  // Permite desplazamiento horizontal
            setScaleEnabled(true)
            invalidate()
        }
    }
}