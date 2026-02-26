package com.example.myfirst

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "safety_table")
data class SafetyCheck(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateTime: String,
    val status: String // e.g., "Pending", "Safe", "Missed"
)