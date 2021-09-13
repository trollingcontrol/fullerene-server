package com.trollingcont.fullerene.server.model

data class PostedMessage(
    val id: Int,
    val chatIndex: Int,
    val body: PostedMessageBody
)