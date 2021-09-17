package com.trollingcont.fullerene.server.repository.adapter

import com.trollingcont.fullerene.server.errorhandling.MessageNotFoundException
import com.trollingcont.fullerene.server.model.Messages
import com.trollingcont.fullerene.server.model.PostedMessage
import com.trollingcont.fullerene.server.model.PostedMessageBody
import com.trollingcont.fullerene.server.repository.BatchInsertUpdateOnDuplicate
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class MessageDatabaseAdapter(
    private val db: Database
    ) {

    companion object {
        const val SELECT_LAST = -1L
        const val SELECT_ALL = -2L
    }

    init {
        transaction(db) {
            SchemaUtils.create(Messages)
        }
    }

    fun getNextMessageId(): Long {
        var autoIncrementId = -1L

        transaction(db) {
            exec("SELECT AUTO_INCREMENT FROM information_schema.TABLES " +
                    "WHERE TABLE_SCHEMA = \"${db.name}\" AND TABLE_NAME = \"${Messages.tableName}\"") { rs ->
                rs.first()
                autoIncrementId = rs.getLong("AUTO_INCREMENT")
            }
        }

        return autoIncrementId
    }

    fun addMessage(chatId: Long, chatIndex: Long, creatorUsername: String, content: String): PostedMessage =
        transaction(db) {
            val result = Messages.insert {
                it[Messages.timePosted] = LocalDateTime.now()
                it[Messages.sourceUser] = creatorUsername
                it[Messages.chatId] = chatId
                it[Messages.content] = content
                it[Messages.chatIndex] = chatIndex
            }

            PostedMessage(
                result[Messages.id].value.toLong(),
                PostedMessageBody(
                    result[Messages.timePosted],
                    result[Messages.sourceUser],
                    result[Messages.chatId],
                    result[Messages.chatIndex],
                    result[Messages.content],
                    result[Messages.isRead]
                )
            )
        }

    fun addMessagesList(messagesList: List<PostedMessage>) {
        transaction(db) {
            Messages.batchInsertOnDuplicateKeyUpdate(
                messagesList,
                listOf(
                    Messages.timePosted,
                    Messages.sourceUser,
                    Messages.chatId,
                    Messages.chatIndex,
                    Messages.content,
                    Messages.isRead
                )
            ) { batch, postedMessage ->
                batch[Messages.id] = postedMessage.id
                batch[Messages.timePosted] = postedMessage.body.timePosted
                batch[Messages.sourceUser] = postedMessage.body.sourceUser
                batch[Messages.chatId] = postedMessage.body.chatId
                batch[Messages.chatIndex] = postedMessage.body.chatIndex
                batch[Messages.content] = postedMessage.body.content
                batch[Messages.isRead] = postedMessage.body.isRead
            }
        }
    }

    fun getMessageById(messageId: Long): PostedMessageBody =
        transaction(db) {
            val result = Messages.select {
                Messages.id eq messageId
            }

            if (result.count() == 0L) {
                throw MessageNotFoundException()
            }

            result.map {
                PostedMessageBody(
                    it[Messages.timePosted],
                    it[Messages.sourceUser],
                    it[Messages.chatId],
                    it[Messages.chatIndex],
                    it[Messages.content],
                    it[Messages.isRead]
                )
            }[0]
        }

    fun getMessageByChatIndex(chatId: Long, chatIndex: Long): PostedMessage =
        transaction(db) {
            val result = Messages.select {
                (Messages.chatId eq chatId) and
                        (Messages.chatIndex eq chatIndex)
            }

            if (result.count() == 0L) {
                throw MessageNotFoundException()
            }

            result.map {
                PostedMessage(
                    it[Messages.id].value,
                    PostedMessageBody(
                        it[Messages.timePosted],
                        it[Messages.sourceUser],
                        it[Messages.chatId],
                        it[Messages.chatIndex],
                        it[Messages.content],
                        it[Messages.isRead]
                    )
                )
            }[0]
        }

    fun markMessageAsRead(messageId: Long): Int =
        transaction(db) {
            Messages.update({ Messages.id eq messageId }) {
                it[Messages.isRead] = true
            }
        }

    fun getChatMessages(chatId: Long, startPoint: Long = SELECT_ALL, count: Long = 1): List<PostedMessage> =
        transaction(db) {
            val query = Messages.select {
                Messages.chatId eq chatId
            }

            when (startPoint) {
                SELECT_ALL -> {
                    query
                        .orderBy(Messages.id, SortOrder.ASC)
                }

                SELECT_LAST -> {
                    query
                        .orderBy(Messages.id, SortOrder.DESC)
                        .limit(count.toInt())
                        .reversed()
                }

                else -> {
                    query
                        .orderBy(Messages.id, SortOrder.ASC)
                        .limit(count.toInt(), startPoint)
                }
            }.map {
                PostedMessage(
                    it[Messages.id].value,
                    PostedMessageBody(
                        it[Messages.timePosted],
                        it[Messages.sourceUser],
                        it[Messages.chatId],
                        it[Messages.chatIndex],
                        it[Messages.content],
                        it[Messages.isRead]
                    )
                )
            }
        }

    fun getChatMessagesCount(chatId: Long): Long =
        transaction(db) {
            Messages.select {
                Messages.chatId eq chatId
            }.count()
        }

    private fun <T : Table, E> T.batchInsertOnDuplicateKeyUpdate(data: List<E>, onDupUpdateColumns: List<Column<*>>, body: T.(BatchInsertUpdateOnDuplicate, E) -> Unit) {
        data.
        takeIf { it.isNotEmpty() }?.
        let {
            val insert = BatchInsertUpdateOnDuplicate(this, onDupUpdateColumns)
            data.forEach {
                insert.addBatch()
                body(insert, it)
            }
            TransactionManager.current().exec(insert)
        }
    }
}