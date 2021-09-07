package com.trollingcont.fullerene.server.model

import java.time.LocalDateTime

data class PostedMessage(
    val id: Int,
    val body: PostedMessageBody
)