package com.example.asb.db.electricaldatalogtest

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ElectricDataDao {
    @Insert
    suspend fun insert(data: ElectricData)

    @Query("SELECT * FROM electric_anomalies ORDER BY timestamp DESC")
    fun getAllAnomalies(): LiveData<List<ElectricData>>

    @Query("SELECT DISTINCT equipmentName FROM electric_anomalies")
    fun getEquipmentNames(): LiveData<List<String>>

    @Query("SELECT * FROM electric_anomalies WHERE equipmentName = :name ORDER BY timestamp DESC")
    fun getAnomaliesByEquipment(name: String): LiveData<List<ElectricData>>
}