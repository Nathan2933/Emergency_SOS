package com.example.myfirst

import androidx.room.*

@Dao
interface SafetyDao {
    // CREATE
    @Insert
    suspend fun insert(check: SafetyCheck)

    // UPDATE
    @Update
    suspend fun update(check: SafetyCheck)

    // READ
    @Query("SELECT * FROM safety_table ORDER BY id DESC")
    suspend fun getAllChecks(): List<SafetyCheck>

    // DELETE
    @Delete
    suspend fun delete(check: SafetyCheck)
}