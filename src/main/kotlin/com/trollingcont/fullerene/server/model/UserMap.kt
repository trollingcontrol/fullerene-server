package com.trollingcont.fullerene.server.model

data class UserMap(
    val passwordHash: String,
    val salt: String
)
