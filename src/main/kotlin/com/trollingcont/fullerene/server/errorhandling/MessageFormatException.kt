package com.trollingcont.fullerene.server.errorhandling

import com.trollingcont.fullerene.server.repository.manager.MessageManager

class MessageFormatException(private val code: MessageManager.MessageErrors) : Exception() {
    fun code() = code
}