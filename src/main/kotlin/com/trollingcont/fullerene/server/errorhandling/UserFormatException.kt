package com.trollingcont.fullerene.server.errorhandling

import com.trollingcont.fullerene.server.repository.manager.UserManager

class UserFormatException(private val code: UserManager.UserDataFormatErrors) : Exception() {
    fun code() = code
}