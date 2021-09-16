package com.trollingcont.fullerene.server.repository.adapter

import com.trollingcont.fullerene.server.model.DefaultChatAcl
import com.trollingcont.fullerene.server.model.User
import com.trollingcont.fullerene.server.model.UserAcl
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class AclDatabaseAdapter(
    private val db: Database
) {
    init {
        transaction(db) {
            SchemaUtils.create(DefaultChatAcl)
            SchemaUtils.create(UserAcl)
        }
    }

    /*
    * If username = null then chat default ACL is affected
    * */

    fun addChatAclRight(chatId: Long, rightName: String, state: Boolean, username: String? = null) {
        transaction(db) {
            if (username == null) {
                DefaultChatAcl.insert {
                    it[DefaultChatAcl.chatId] = chatId
                    it[DefaultChatAcl.rightName] = rightName
                    it[DefaultChatAcl.state] = state
                }
            }

            else {
                UserAcl.insert {
                    it[UserAcl.chatId] = chatId
                    it[UserAcl.username] = username
                    it[UserAcl.rightName] = rightName
                    it[UserAcl.state] = state
                }
            }
        }
    }

    fun setChatAclRight(chatId: Long, rightName: String, state: Boolean, username: String? = null) {
        transaction(db) {
            if (username == null) {
                DefaultChatAcl.update({
                    (DefaultChatAcl.chatId eq chatId) and
                            (DefaultChatAcl.rightName eq rightName)
                }) {
                    it[DefaultChatAcl.state] = state
                }
            }

            else {
                UserAcl.update({
                    (UserAcl.chatId eq chatId) and
                            (UserAcl.rightName eq rightName) and
                            (UserAcl.username eq username)
                }) {
                    it[UserAcl.state] = state
                }
            }
        }
    }

    fun getChatAclRight(chatId: Long, rightName: String, username: String? = null): Boolean? =
        transaction(db) {
            if (username == null) {
                val query = DefaultChatAcl.select {
                    (DefaultChatAcl.chatId eq chatId) and
                            (DefaultChatAcl.rightName eq rightName)
                }

                if (query.count() > 0) {
                    query.map {
                        it[DefaultChatAcl.state]
                    }[0]
                }
                else {
                    null
                }
            }

            else {
                val query = UserAcl.select {
                    (UserAcl.chatId eq chatId) and
                            (UserAcl.rightName eq rightName) and
                            (UserAcl.username eq username)
                }

                if (query.count() > 0) {
                    query.map {
                        it[UserAcl.state]
                    }[0]
                }
                else {
                    null
                }
            }
        }

    fun deleteChatAclRight(chatId: Long, rightName: String, username: String? = null): Int =
        transaction(db) {
            if (username == null) {
                DefaultChatAcl.deleteWhere {
                    (DefaultChatAcl.chatId eq chatId) and
                            (DefaultChatAcl.rightName eq rightName)
                }
            }

            else {
                UserAcl.deleteWhere {
                    (UserAcl.chatId eq chatId) and
                            (UserAcl.rightName eq rightName) and
                            (UserAcl.username eq username)
                }
            }
        }
}