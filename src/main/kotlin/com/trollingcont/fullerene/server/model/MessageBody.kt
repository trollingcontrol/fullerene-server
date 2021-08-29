package com.trollingcont.fullerene.server.model

data class MessageBody(
    val sourceUser: String,
    val destinationUser: String,
    val messageText: String
)
