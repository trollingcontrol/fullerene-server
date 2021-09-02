package com.trollingcont.fullerene.server.repository.manager

import com.trollingcont.fullerene.server.repository.BufferedIO
import com.trollingcont.fullerene.server.repository.adapter.MessageDatabaseAdapter
import org.jetbrains.exposed.sql.Database

class MessageManager(
    db: Database
) : BufferedIO {

    private val dbDriver = MessageDatabaseAdapter(db)

    override fun flushInputBuffer() {

    }
}