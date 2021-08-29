package com.trollingcont.fullerene.server.model

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.datetime

object Users : IntIdTable() {
    val name = text("name").uniqueIndex()
    val passwordHash = text("passwordHash")
    val salt = text("salt")
}

object Messages : IntIdTable() {
    val time = datetime("timePosted")
    val sourceUser = text("sourceUser")
    val chatId = integer("chatId")
    val content = text("content")
    val isRead = bool("isRead").default(false)
}

object ChatData : IntIdTable() {
    val timeUpdated = datetime("timeUpdated")
}

object ChatParticipants : Table() {
    val chatId = integer("chatId").references(ChatData.id)
    val user = text("user")
}