package com.example.asb.faults.chart

import android.graphics.Color
import android.util.Log
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Locale

class ChartHelper {

    fun getChartData(rawData: String): List<StackedBarEntry> {
        val dateFormat = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US)
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.US)

        return rawData.split(", ").mapNotNull { item ->
            try {
                // Dividir primero por el primer "---" que separa ID del resto
                val idSplit = item.split("---", limit = 2)
                if (idSplit.size != 2) return@mapNotNull null

                // Dividir el resto por el primer "-" que separa código del registro
                val restParts = idSplit[1].split("-", limit = 3)
                if (restParts.size != 3) return@mapNotNull null

                val codigo = restParts[0]
                val registro = restParts[1]
                val fechaTexto = restParts[2].trim()

                val date = dateFormat.parse(fechaTexto) ?: return@mapNotNull null
                val monthYear = monthFormat.format(date)

                StackedBarEntry(
                    timeLabel = monthYear,
                    bomba1Count = if (registro.contains("BOMBA 1")) 1 else 0,
                    bomba2Count = if (registro.contains("BOMBA 2")) 1 else 0,
                    sistemaCount = if (codigo.startsWith("120") || codigo.startsWith("121")) 1 else 0
                )
            } catch (e: Exception) {
                Log.e("ChartHelper", "Error parsing item: $item", e)
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