package com.trollingcont.fullerene.server.repository

import com.trollingcont.fullerene.server.model.ChatData
import com.trollingcont.fullerene.server.model.ChatParticipants
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class ChatDatabaseDriver(
    private val db: Database
) {
    companion object {
        const val SELECT_LAST = -1L
        const val SELECT_ALL = -2L
    }

    init {
        transaction(db) {
            SchemaUtils.create(ChatParticipants)
            SchemaUtils.create(ChatData)
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

    fun createChat(updateTime: LocalDateTime): Int =
        transaction(db) {
            ChatData.insert {
                it[ChatData.timeUpdated] = updateTime
            }[ChatData.id].value
        }

    fun setChatUpdateTime(chatId: Int, updateTime: LocalDateTime): Int =
        transaction(db) {
            ChatData.update({ ChatData.id eq chatId }) {
                it[ChatData.timeUpdated] = updateTime
            }
        }

    fun getChats(): List<Pair<Int, String>> =
        transaction(db) {
            ChatParticipants.selectAll().map {
                Pair(
                    it[ChatParticipants.chatId],
                    it[ChatParticipants.user]
                )
            }
        }

    fun isChatParticipant(chatId: Int, username: String): Boolean =
        transaction(db) {
            ChatParticipants.select {
                (ChatParticipants.chatId eq chatId) and
                        (ChatParticipants.user eq username)
            }.count() != 0L
        }

    fun getChatParticipants(chatId: Int): List<String> =
        transaction(db) {
            ChatParticipants.select {
                ChatParticipants.chatId eq chatId
            }.map {
                it[ChatParticipants.user]
            }
        }

    fun getUserChats(username: String, startPoint: Long = SELECT_ALL, count: Int = 1): List<Int> =
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
                        .limit(count)
                        .reversed()
                }

                else -> {
                    query
                        .orderBy(ChatData.timeUpdated, SortOrder.ASC)
                        .limit(count, startPoint)
                }
            }.map {
                it[ChatParticipants.chatId]
            }
        }

    fun addChatParticipant(chatId: Int, username: String) {
        transaction(db) {
            ChatParticipants.insert {
                it[ChatParticipants.chatId] = chatId
                it[ChatParticipants.user] = username
            }
        }
    }

    fun removeChatParticipant(chatId: Int, username: String): Int =
        transaction(db) {
            ChatParticipants.deleteWhere {
                (ChatParticipants.chatId eq chatId) and
                        (ChatParticipants.user eq username)
            }
        }
}