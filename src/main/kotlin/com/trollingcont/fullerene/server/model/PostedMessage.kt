package com.trollingcont.fullerene.server.model

import java.time.LocalDateTime

data class PostedMessage(
    val id: Int,
    val timePosted: LocalDateTime,
    val sourceUser: String,
    val chatId: Int,
    val content: String,
    val isRead: Boolean
)
