package com.example.asb.faults.chart

// Versión simplificada con Parcelable para pasar entre Activities
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StackedBarEntry(
    val timeLabel: String,
    val bomba1Count: Int,
    val bomba2Count: Int,
    val sistemaCount: Int
) : Parcelable