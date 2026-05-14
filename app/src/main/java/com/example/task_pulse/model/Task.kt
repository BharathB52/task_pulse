package com.example.task_pulse.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val deadline: Long, // Combines date and time
    val priority: String, // "High", "Medium", "Low"
    val category: String = "Personal", // "Work", "Study", "Personal"
    val isCompleted: Boolean = false,
    val repeatInterval: String = "None" // "None", "Daily", "Weekly", "Custom"
)
