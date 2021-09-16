package com.trollingcont.fullerene.server.repository.adapter

import com.trollingcont.fullerene.server.model.ChatData
import com.trollingcont.fullerene.server.model.ChatParticipants
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class ChatDatabaseAdapter(
    private val db: Database
) {
    companion object {
        const val SELECT_LAST = -1L
        const val SELECT_ALL = -2L
    }

    init {
        transaction(db) {
            SchemaUtils.create(ChatData)
            SchemaUtils.create(ChatParticipants)
        }
    }

    fun getNextChatId(): Int {
        var autoIncrementId = -1

        transaction(db) {
            exec("SELECT AUTO_INCREMENT FROM information_schema.TABLES " +
                    "WHERE TABLE_SCHEMA = \"${db.name}\" AND TABLE_NAME = \"${ChatData.tableName}\"") { rs ->
                rs.first()
                autoIncrementId = rs.getInt("AUTO_INCREMENT")
            }
        }

        return autoIncrementId
    }

    fun createChat(creator: String, updateTime: LocalDateTime): Long =
        transaction(db) {
            ChatData.insert {
                it[ChatData.timeUpdated] = updateTime
                it[ChatData.creator] = creator
            }[ChatData.id].value
        }

    fun setChatUpdateTime(chatId: Long, updateTime: LocalDateTime): Long =
        transaction(db) {
            ChatData.update({ ChatData.id eq chatId }) {
                it[ChatData.timeUpdated] = updateTime
            }.toLong()
        }

    fun getChats(): List<Pair<Long, String>> =
        transaction(db) {
            ChatParticipants.selectAll().map {
                Pair(
                    it[ChatParticipants.chatId],
                    it[ChatParticipants.user]
                )
            }
        }

    fun isChatParticipant(chatId: Long, username: String): Boolean =
        transaction(db) {
            ChatParticipants.select {
                (ChatParticipants.chatId eq chatId) and
                        (ChatParticipants.user eq username)
            }.count() != 0L
        }

    fun getChatParticipants(chatId: Long): List<String> =
        transaction(db) {
            ChatParticipants.select {
                ChatParticipants.chatId eq chatId
            }.map {
                it[ChatParticipants.user]
            }
        }

    fun getUserChats(username: String, startPoint: Long = SELECT_ALL, count: Long = 1): List<Long> =
        transaction(db) {
            val query = ChatParticipants.join(ChatData, JoinType.INNER)
                .slice(ChatParticipants.chatId)
                .select {
                    ChatParticipants.user eq username
                }

            when (startPoint) {
                SELECT_ALL -> {
                    query
                        .orderBy(ChatData.timeUpdated, SortOrder.ASC)
                }

                SELECT_LAST -> {
                    query
                        .orderBy(ChatData.timeUpdated, SortOrder.DESC)
                        .limit(count.toInt())
                        .reversed()
                }

                else -> {
                    query
                        .orderBy(ChatData.timeUpdated, SortOrder.ASC)
                        .limit(count.toInt(), startPoint)
                }
            }.map {
                it[ChatParticipants.chatId]
            }
        }

    fun addChatParticipant(chatId: Long, username: String) {
        transaction(db) {
            ChatParticipants.insert {
                it[ChatParticipants.chatId] = chatId
                it[ChatParticipants.user] = username
            }
        }
    }

    fun deleteChatParticipant(chatId: Long, username: String): Long =
        transaction(db) {
            ChatParticipants.deleteWhere {
                (ChatParticipants.chatId eq chatId) and
                        (ChatParticipants.user eq username)
            }.toLong()
        }
}