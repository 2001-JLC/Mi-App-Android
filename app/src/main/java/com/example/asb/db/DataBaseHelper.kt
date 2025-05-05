package com.example.asb.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "MonitoringDB"
        private const val DATABASE_VERSION = 7

        // Tablas
        const val TABLE_CLIENT_PROJECTS = "client_projects"
        const val TABLE_USERS = "users"

        // Columnas
        const val COL_ID = "id"
        const val COL_USER_ID = "user_id"
        const val COL_ORDER_ID = "order_id"
        const val COL_PROJECT_TYPE = "project_type"
        const val COL_PROJECT_NAME = "project_name"
        const val COL_USERNAME = "username"
        const val COL_PASSWORD = "password"
        const val COL_FULL_NAME = "full_name"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_USERS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USERNAME TEXT UNIQUE NOT NULL,
                $COL_PASSWORD TEXT NOT NULL,
                $COL_FULL_NAME TEXT NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_CLIENT_PROJECTS (
                $COL_USER_ID TEXT NOT NULL,
                $COL_ORDER_ID TEXT NOT NULL,
                $COL_PROJECT_TYPE TEXT NOT NULL,
                $COL_PROJECT_NAME TEXT,
                PRIMARY KEY ($COL_USER_ID, $COL_ORDER_ID)
            )
        """)

        insertInitialData(db)
    }

    private fun insertInitialData(db: SQLiteDatabase) {
        // Insertar usuarios
        listOf(
            ContentValues().apply {  // Jesús Leyva
                put(COL_ID, 1)
                put(COL_USERNAME, "Leyva")
                put(COL_PASSWORD, "123")
                put(COL_FULL_NAME, "Jesus Leyva")
            },
            ContentValues().apply {  // Franco
                put(COL_ID, 2)
                put(COL_USERNAME, "Franco")
                put(COL_PASSWORD, "456")
                put(COL_FULL_NAME, "Franco Pérez")
            },
            ContentValues().apply {  // Nuevo usuario Carlos
                put(COL_ID, 3)
                put(COL_USERNAME, "Carlos")
                put(COL_PASSWORD, "789")
                put(COL_FULL_NAME, "Carlos SVV")
            }
        ).forEach { db.insert(TABLE_USERS, null, it) }

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CLIENT_PROJECTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }
}