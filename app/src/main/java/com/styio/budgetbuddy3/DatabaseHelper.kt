package com.styio.budgetbuddy3

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MyDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "budgetbuddy2.db"
        private const val DATABASE_VERSION = 1
    }

    // Create your tables in the onCreate method
    override fun onCreate(db: SQLiteDatabase) {
        val createBudget = """
            CREATE TABLE IF NOT EXISTS budget_table (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                category STRING,
                spent DOUBLE,
                total DOUBLE,
                percent INTEGER
            )
        """.trimIndent()
        db.execSQL(createBudget)
        val createCurrentTable = """
            CREATE TABLE IF NOT EXISTS current_date_table (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                month INTEGER,
                year, INTEGER                
            )
        """.trimIndent()
        db.execSQL(createCurrentTable)
        val createHistoryTable = """
            CREATE TABLE IF NOT EXISTS history_table (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                category String,
                spent DOUBLE,
                total DOUBLE,
                percent INTEGER,
                month INTEGER,
                year, INTEGER
            )
        """.trimIndent()
        db.execSQL(createHistoryTable)


    }

    // Upgrade the database if the version is increased
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS budget_table")
        db.execSQL("DROP TABLE IF EXISTS history_table")
        db.execSQL("DROP TABLE IF EXISTS current_date_table")
        onCreate(db)
    }
}
