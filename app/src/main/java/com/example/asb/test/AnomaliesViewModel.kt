package com.example.asb.test

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.asb.db.electricaldatalogtest.ElectricData
import com.example.asb.db.electricaldatalogtest.ElectricDataDatabase

class AnomaliesViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = ElectricDataDatabase.getDatabase(application).electricDataDao()

    val equipmentNames: LiveData<List<String>> = dao.getEquipmentNames()

    fun getAnomaliesForEquipment(name: String): LiveData<List<ElectricData>> {
        return dao.getAnomaliesByEquipment(name)
    }
}