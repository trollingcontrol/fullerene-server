package com.trollingcont.fullerene.server.model

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.datetime

object Users : Table() {
    val name = varchar("name", 64)
    val passwordHash = text("passwordHash")
    val salt = text("salt")
    override val primaryKey = PrimaryKey(name)
}

object ChatData : LongIdTable() {
    val timeUpdated = datetime("timeUpdated")
    val creator = varchar("name", 64)
}

object Messages : LongIdTable() {
    val timePosted = datetime("timePosted")
    val sourceUser = varchar("name", 64)
    val chatId = long("chatId")
    val chatIndex = long("chatIndex")
    val content = text("content")
    val isRead = bool("isRead").default(false)
}

object ChatParticipants : Table() {
    val chatId = long("chatId")
    val user = varchar("name", 64)
}

object DefaultChatAcl : Table() {
    val chatId = long("chatId")
    val rightName = text("rightName")
    val state = bool("state")
}

object UserAcl : Table() {
    val chatId = long("chatId")
    val username = varchar("name", 64)
    val rightName = text("rightName")
    val state = bool("state")
}