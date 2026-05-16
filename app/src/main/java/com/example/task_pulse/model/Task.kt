package com.example.task_pulse.model

import com.google.firebase.firestore.PropertyName

data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val deadline: Long = 0,
    val priority: String = "Low",
    val category: String = "Personal",
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    val repeatInterval: String = "None"
)
