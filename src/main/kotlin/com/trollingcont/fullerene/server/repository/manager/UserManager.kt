package com.trollingcont.fullerene.server.repository.manager

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.trollingcont.fullerene.server.errorhandling.UserAlreadyExistsException
import com.trollingcont.fullerene.server.errorhandling.UserFormatException
import com.trollingcont.fullerene.server.errorhandling.UserNotFoundException
import com.trollingcont.fullerene.server.model.User
import com.trollingcont.fullerene.server.model.UserMap
import com.trollingcont.fullerene.server.repository.BufferedIO
import com.trollingcont.fullerene.server.repository.adapter.UserDatabaseAdapter
import org.jetbrains.exposed.sql.*
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import kotlin.collections.HashMap

class UserManager(
    private val db: Database,
    private val hs256secret: String,
    private val jwtIssuer: String
    ) : BufferedIO {

    private val dbDriver = UserDatabaseAdapter(db)
    private val writeBuffer = HashMap<String, UserMap>()
    private val readBuffer = dbDriver.getUsersList()

    enum class UserDataFormatErrors(val errorCode: Int) {
        NO_ERROR(0),
        USERNAME_EMPTY(1),
        USERNAME_TOO_SHORT(2),
        USERNAME_INVALID_CHARS(3),
        PASSWORD_EMPTY(4),
        PASSWORD_TOO_SHORT(5),
        PASSWORD_INVALID_CHARS(6),
        USERNAME_TOO_LONG(7),
        PASSWORD_TOO_LONG(8)
    }

    fun addUser(user: User) {
        val errorCode = validateUserData(user)

        if (errorCode != UserDataFormatErrors.NO_ERROR) {
            throw UserFormatException(errorCode)
        }

        if (isUsernameUsed(user.name)) {
            throw UserAlreadyExistsException()
        }

        val passwordSalt = generateRandomString(32)
        var newUserPasswordHash = user.password + passwordSalt

        for (i in 1..HASHING_COUNT) {
            newUserPasswordHash = generateStringHash(newUserPasswordHash)
        }

        writeBuffer[user.name] = UserMap(newUserPasswordHash, passwordSalt)
    }

    fun generateToken(user: User): String {
        val errorCode = validateUserData(user)

        if (errorCode != UserDataFormatErrors.NO_ERROR) {
            throw UserFormatException(errorCode)
        }

        var userMap = writeBuffer[user.name] ?: readBuffer[user.name]

        if (userMap == null) {
            val registeredUser = dbDriver.getUserByName(user.name)
            userMap = UserMap(registeredUser.passwordHash, registeredUser.salt)
        }

        var calculatedPasswordHash = user.password + userMap.salt

        for (i in 1..HASHING_COUNT) {
            calculatedPasswordHash = generateStringHash(calculatedPasswordHash)
        }

        if (userMap.passwordHash != calculatedPasswordHash) {
            throw UserNotFoundException()
        }

        val algorithm = Algorithm.HMAC256(hs256secret)

        val calendar = Calendar.getInstance()
        val currentTime = calendar.time
        calendar.add(Calendar.HOUR, 48)
        val expirationTime = calendar.time

        return JWT.create()
            .withIssuer(jwtIssuer)
            .withIssuedAt(currentTime)
            .withExpiresAt(expirationTime)
            .withSubject(user.name)
            .withJWTId(generateRandomString(16))
            .sign(algorithm)
    }

    fun isValidToken(token: String, checkedUsername: String? = null) =
        try {
            val algorithm = Algorithm.HMAC256(hs256secret)

            val verifier = JWT.require(algorithm)
                .withIssuer(jwtIssuer)
                .build()

            val decodedJwt = verifier.verify(token)

            !(checkedUsername != null && decodedJwt.subject != checkedUsername)
        } catch (exc: JWTVerificationException) {
            false
        }

    fun isUsernameUsed(username: String): Boolean {
        return writeBuffer.containsKey(username) || readBuffer.containsKey(username)
    }

    override fun flushInputBuffer() {
        dbDriver.addUsers(writeBuffer)

        readBuffer.putAll(writeBuffer)
        writeBuffer.clear()
    }

    companion object {
        fun validateUserData(user: User): UserDataFormatErrors =
            when {
                user.name.isEmpty() -> UserDataFormatErrors.USERNAME_EMPTY
                user.name.length < 3 -> UserDataFormatErrors.USERNAME_TOO_SHORT
                user.name.length > 32 -> UserDataFormatErrors.USERNAME_TOO_LONG
                user.name.indexOf(" ") != -1 -> UserDataFormatErrors.USERNAME_INVALID_CHARS
                user.name.isEmpty() -> UserDataFormatErrors.PASSWORD_EMPTY
                user.password.length < 6 -> UserDataFormatErrors.PASSWORD_TOO_SHORT
                user.password.length > 32 -> UserDataFormatErrors.PASSWORD_TOO_LONG
                user.password.indexOf(" ") != -1 -> UserDataFormatErrors.PASSWORD_INVALID_CHARS
                else -> UserDataFormatErrors.NO_ERROR
            }

        private const val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        private const val charsLength = chars.length
        private const val HASHING_COUNT = 32

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