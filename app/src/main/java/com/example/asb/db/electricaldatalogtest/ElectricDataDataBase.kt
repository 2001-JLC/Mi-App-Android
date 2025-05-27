package com.example.asb.db.electricaldatalogtest

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ElectricData::class], version = 1)
abstract class ElectricDataDatabase : RoomDatabase() {
    abstract fun electricDataDao(): ElectricDataDao

    companion object {
        @Volatile
        private var INSTANCE: ElectricDataDatabase? = null

        fun getDatabase(context: Context): ElectricDataDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ElectricDataDatabase::class.java,
                    "electric_data.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}