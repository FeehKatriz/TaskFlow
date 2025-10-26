package com.example.taskflow.data.model

data class Comment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)