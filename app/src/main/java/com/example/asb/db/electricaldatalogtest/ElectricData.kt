package com.example.asb.db.electricaldatalogtest

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "electric_anomalies")
data class ElectricData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String, // "VOLTAGE_LOW" o "PRESSURE_LOW"
    val value: Double,
    val equipmentName: String
)