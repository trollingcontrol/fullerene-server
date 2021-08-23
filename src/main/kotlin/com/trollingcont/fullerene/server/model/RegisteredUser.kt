package com.trollingcont.fullerene.server.model

data class RegisteredUser(
    val id: Int,
    val name: String,
    val passwordHash: String,
    val salt: String
)