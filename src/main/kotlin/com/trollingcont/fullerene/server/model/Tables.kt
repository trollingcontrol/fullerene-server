package com.trollingcont.fullerene.server.model

import org.jetbrains.exposed.dao.id.IntIdTable

object Users : IntIdTable() {
    val name = text("name").uniqueIndex()
    val passwordHash = text("passwordHash")
    val salt = text("salt")
}