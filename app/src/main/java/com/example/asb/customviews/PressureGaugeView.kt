package com.example.asb.customviews

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

// Clase personalizada que extiende de View para crear un medidor de presión
class PressureGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // Inicializamos el objeto Paint para dibujar
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER // Alineación del texto al centro
    }
    // Rectángulo que define el área de dibujo
    private val rectF = RectF()

    // Presión actual y límites
    private var currentPressure = 2.5f
    private val minPressure = 1f
    private val maxPressure = 4f
    private val minOperatingPressure = 2.4f
    private val maxOperatingPressure = 3.2f
    // Colores optimizados para diferentes estados de presión
    private val lowColor = Color.parseColor("#2196F3")  // Azul (<2.4)
    private val normalColor = Color.parseColor("#4CAF50") // Verde (2.4-3.2)
    private val highColor = Color.parseColor("#F44336")  // Rojo (>3.2)
    private val needleColor = Color.parseColor("#FF5722") // Naranja
    private val bgColor = Color.parseColor("#BDBDBD")    // Gris claro de fondo

    // Metodo para establecer la presión y forzar un redibujo
    fun setPressure(pressure: Float) {
        currentPressure = pressure.coerceIn(minPressure, maxPressure) // Asegura que la presión esté dentro de los límites
        invalidate() // Solicita un redibujo de la vista
    }
    // Metodo que se llama para dibujar la vista
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height * 0.8f  // Posición más baja para la media luna
        val radius = width.coerceAtMost(height) * 0.7f // Radio del medidor\

        // Configurar el área de dibujo (media luna inferior)
        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        // Dibujar el fondo del medidor, la aguja y el texto de presión
        drawGaugeBackground(canvas)
        drawNeedle(canvas, centerX, centerY, radius)
        drawPressureText(canvas, centerX)

        drawLabel(canvas, centerX) //para drawlabel
    }

    // Metodo para dibujar el fondo del medidor
    private fun drawGaugeBackground(canvas: Canvas) {
        // Fondo semicircular
        paint.color = bgColor
        paint.style = Paint.Style.FILL
        canvas.drawArc(rectF, 180f, 180f, true, paint)
        // Escala de colores (solo borde)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = rectF.width() * 0.04f  // Grosor relativo al tamaño
        // Zona baja (azul)
        paint.color = lowColor
        canvas.drawArc(rectF, 180f, pressureToAngle(minOperatingPressure), false, paint)
        // Zona normal (verde)
        paint.color = normalColor
        canvas.drawArc(rectF, 180f + pressureToAngle(minOperatingPressure),
            pressureToAngle(maxOperatingPressure) - pressureToAngle(minOperatingPressure), false, paint)
        // Zona alta (rojo)
        paint.color = highColor
        canvas.drawArc(rectF, 180f + pressureToAngle(maxOperatingPressure),
            180f - pressureToAngle(maxOperatingPressure), false, paint)
    }
    // Metodo para dibujar la aguja del medidor
    private fun drawNeedle(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        val angle = 180 + (currentPressure / maxPressure * 180) // Calcular el ángulo de la aguja
        val needleLength = radius * 0.8f // Longitud de la aguja
        // Crear un camino para la aguja triangular
        val path = Path().apply {
            val tipX = centerX + needleLength * cos(Math.toRadians(angle.toDouble())).toFloat() // Punta de la aguja
            val tipY = centerY + needleLength * sin(Math.toRadians(angle.toDouble())).toFloat() // Punta de la aguja
            moveTo(centerX, centerY - 6f)  // Base superior más delgada
            lineTo(tipX, tipY)             // Punta
            lineTo(centerX, centerY + 10f)  // Base inferior más delgada
            close() // Cerrar el camino
        }
        // Dibujar la aguja
        paint.style = Paint.Style.FILL
        paint.color = needleColor
        canvas.drawPath(path, paint)
    }

    // Metodo para dibujar el texto de presión
    private fun drawPressureText(canvas: Canvas, centerX: Float) {
        // Valor numérico grande
        paint.style = Paint.Style.FILL
        paint.textSize = width * 0.15f // Tamaño del texto
        paint.color = when {
            currentPressure > maxOperatingPressure -> highColor // Color para alta presión
            currentPressure < minOperatingPressure -> lowColor // Color para baja presión
            else -> normalColor // Color para presión normal
        }
        // Dibujar el valor de presión
        canvas.drawText("%.2f".format(currentPressure), centerX, height * 0.5f, paint)
        // Unidades
        paint.textSize = width * 0.06f // Tamaño del texto para unidades
        paint.color = Color.DKGRAY // Color para las unidades
        canvas.drawText("kg/cm²", centerX, height * 0.6f, paint) // Dibujar las unidades
    }

    // Metodo para convertir la presión a un ángulo
    private fun pressureToAngle(pressure: Float): Float {
        return (pressure / maxPressure * 180f) // Conversión de presión a ángulo
    }

    private fun drawLabel(canvas: Canvas, centerX: Float) { //Para identificar el nombre debajo de la media luna
        paint.style = Paint.Style.FILL
        paint.textSize = width * 0.06f  // Tamaño relativo al ancho del medidor
        paint.color = Color.DKGRAY      // Color del texto
        canvas.drawText(
            "Presión del sistema",     // Texto a mostrar
            centerX,                   // Centrado horizontal
            height * 0.90f,            // Posición vertical (ajusta este valor según necesites)
            paint
        )
    }
}