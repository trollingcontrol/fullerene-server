package com.trollingcont.fullerene.server.model

import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = text("name").uniqueIndex()
    val passwordHash = text("passwordHash")
    val salt = text("salt")
    override val primaryKey = PrimaryKey(id)
}