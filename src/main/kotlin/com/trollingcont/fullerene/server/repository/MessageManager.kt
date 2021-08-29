package com.trollingcont.fullerene.server.repository

import org.jetbrains.exposed.sql.Database

class MessageManager(
    db: Database
) : BufferedIO {

    private val dbDriver = MessageDatabaseDriver(db)

    override fun flushInputBuffer() {

    }
}