package com.trollingcont.fullerene.server.repository

import com.trollingcont.fullerene.server.model.User
import com.trollingcont.fullerene.server.model.UserMap
import org.jetbrains.exposed.sql.Database
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import kotlin.collections.HashMap

class UserManager(db: Database) : BufferedIO {

    private val writeBuffer = HashMap<String, UserMap>()
    private val readBuffer = HashMap<String, UserMap>()

    enum class UserDataFormatErrors(val errorCode: Int) {
        NO_ERROR(0),
        USERNAME_EMPTY(1),
        USERNAME_TOO_SHORT(2),
        USERNAME_INVALID_CHARS(3),
        PASSWORD_EMPTY(4),
        PASSWORD_TOO_SHORT(5),
        PASSWORD_INVALID_CHARS(6)
    }

    fun addUser(user: User) {

    }

    fun generateToken(user: User): String {
        return "A"
    }

    fun isValidToken(token: String, checkedUsername: String? = null): Boolean {
        return true
    }

    fun isUsernameUsed(username: String): Boolean {
        return true
    }

    override fun flushInputBuffer() {}

    companion object {
        fun validateUserData(user: User): UserDataFormatErrors =
            when {
                user.name.isEmpty() -> UserDataFormatErrors.USERNAME_EMPTY
                user.name.length < 3 -> UserDataFormatErrors.USERNAME_TOO_SHORT
                user.name.indexOf(" ") != -1 -> UserDataFormatErrors.USERNAME_INVALID_CHARS
                user.name.isEmpty() -> UserDataFormatErrors.PASSWORD_EMPTY
                user.password.length < 6 -> UserDataFormatErrors.PASSWORD_TOO_SHORT
                user.password.indexOf(" ") != -1 -> UserDataFormatErrors.PASSWORD_INVALID_CHARS
                else -> UserDataFormatErrors.NO_ERROR
            }

        private const val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        private const val charsLength = chars.length
        private const val hashingCount = 16

        fun generateRandomString(length: Int): String {
            val secureRandom = SecureRandom()
            val str = StringBuilder()

            for (i in 0..length) {
                str.append(chars[secureRandom.nextInt(charsLength - 1)])
            }

            return str.toString()
        }

        private fun generateStringHash(sourceStr: String): String {
            val bytes = MessageDigest
                .getInstance("SHA-256")
                .digest(sourceStr.toByteArray())

            return printHexBinary(bytes).toUpperCase(Locale.ROOT)
        }

        private val hexChars = "0123456789ABCDEF".toCharArray()

        private fun printHexBinary(data: ByteArray): String {
            val r = StringBuilder(data.size * 2)
            data.forEach { b ->
                val i = b.toInt()
                r.append(hexChars[i shr 4 and 0xF])
                r.append(hexChars[i and 0xF])
            }
            return r.toString()
        }
    }
}