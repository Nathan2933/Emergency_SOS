package com.example.myfirst

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "UserDB"
        private const val DATABASE_VERSION = 1
        private const val TABLE_USERS = "Users"
        private const val COLUMN_ID = "id"
        private const val COLUMN_FULLNAME = "fullname"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PHONE = "phone"
        private const val COLUMN_PASSWORD = "password"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_USERS ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_FULLNAME TEXT, "
                + "$COLUMN_EMAIL TEXT UNIQUE, "
                + "$COLUMN_PHONE TEXT, "
                + "$COLUMN_PASSWORD TEXT)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun registerUser(fullName: String, email: String, phone: String, password: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_FULLNAME, fullName)
        values.put(COLUMN_EMAIL, email)
        values.put(COLUMN_PHONE, phone)
        values.put(COLUMN_PASSWORD, password)

        val result = db.insert(TABLE_USERS, null, values)
        db.close()
        return result != -1L
    }

    fun checkUser(email: String, password: String): Boolean {
        val db = this.readableDatabase
        var result = false
        try {
            val cursor: Cursor = db.rawQuery(
                "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?",
                arrayOf(email, password)
            )

            if (cursor.moveToFirst()) {
                result = true
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return result
    }

}
