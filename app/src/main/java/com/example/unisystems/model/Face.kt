package com.example.unisystems.model


class Face(
    val id: String,
    val faceUrl: String,
    val timestamp: Any = System.currentTimeMillis(),
    val name: String,
    val surname: String,
    val place: String
)