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

        // Insertar proyectos
        val projects = listOf(
            ProjectData("001", "0001", "02", "Pozo de agua"),  // Leyva
            ProjectData("001", "0002", "03", "Sistema hidroneumático"),  // Leyva
            ProjectData("002", "0003", "04", "Cárcamo de bombeo"),  // Franco
            ProjectData("003", "0004", "01", "SVV Industrial")  // Nuevo proyecto SVV para Carlos (user_id = 003)
        )

        projects.forEach { project ->
            db.insert(TABLE_CLIENT_PROJECTS, null,
                ContentValues().apply {
                    put(COL_USER_ID, project.userId)
                    put(COL_ORDER_ID, project.orderId)
                    put(COL_PROJECT_TYPE, project.type)
                    put(COL_PROJECT_NAME, project.name)
                }
            )
        }
    }

    // Clase de apoyo para mejor legibilidad
    private data class ProjectData(
        val userId: String,
        val orderId: String,
        val type: String,
        val name: String
    )

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CLIENT_PROJECTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // Métodos optimizados
    fun validateUser(username: String, password: String): Boolean {
        return readableDatabase.query(
            TABLE_USERS,
            arrayOf(COL_ID),
            "$COL_USERNAME = ? AND $COL_PASSWORD = ?",
            arrayOf(username, password),
            null, null, null
        ).use { it.count > 0 }
    }

    fun getUserId(username: String): Long {
        readableDatabase.query(
            TABLE_USERS,
            arrayOf(COL_ID),
            "$COL_USERNAME = ?",
            arrayOf(username),
            null, null, null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else -1L
        }
    }

    fun getProjectInfo(userId: String, orderId: String): Pair<String, String>? {
        readableDatabase.query(
            TABLE_CLIENT_PROJECTS,
            arrayOf(COL_PROJECT_TYPE, COL_PROJECT_NAME),
            "$COL_USER_ID = ? AND $COL_ORDER_ID = ?",
            arrayOf(userId, orderId),
            null, null, null
        ).use { cursor ->
            return if (cursor.moveToFirst()) {
                cursor.getString(0) to cursor.getString(1)
            } else null
        }
    }
}