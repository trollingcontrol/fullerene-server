package com.trollingcont.fullerene.server.model

import java.time.LocalDateTime

class PostedMessageBody(
    val timePosted: LocalDateTime,
    val sourceUser: String,
    val chatId: Long,
    val chatIndex: Long,
    val content: String,
    val isRead: Boolean
)
