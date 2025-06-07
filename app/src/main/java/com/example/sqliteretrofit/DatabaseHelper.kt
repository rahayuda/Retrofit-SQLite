package com.example.sqliteretrofit

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "storage", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE users (
                id TEXT PRIMARY KEY,
                name TEXT,
                email TEXT,
                is_synced INTEGER DEFAULT 0
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    fun insertUser(user: User, isSynced: Boolean = false): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", user.id)
            put("name", user.name)
            put("email", user.email)
            put("is_synced", if (isSynced) 1 else 0)
        }
        return db.insert("users", null, values)
    }

    fun updateUser(user: User, isSynced: Boolean = false): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", user.name)
            put("email", user.email)
            put("is_synced", if (isSynced) 1 else 0)
        }
        return db.update("users", values, "id = ?", arrayOf(user.id))
    }

    fun deleteUser(id: String): Int {
        val db = writableDatabase
        return db.delete("users", "id = ?", arrayOf(id))
    }

    fun getAllUsers(): List<User> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM users", null)
        val users = mutableListOf<User>()

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val email = cursor.getString(cursor.getColumnIndexOrThrow("email"))
                users.add(User(id, name, email))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return users
    }

}
