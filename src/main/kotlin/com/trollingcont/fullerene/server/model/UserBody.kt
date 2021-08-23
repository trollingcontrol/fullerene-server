package com.trollingcont.fullerene.server.model

data class UserBody(
    val name: String,
    val passwordHash: String,
    val salt: String
)
