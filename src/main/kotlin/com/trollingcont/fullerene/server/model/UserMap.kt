package com.trollingcont.fullerene.server.model

data class UserMap(
    val id: Int,
    val passwordHash: String,
    val salt: String
)
