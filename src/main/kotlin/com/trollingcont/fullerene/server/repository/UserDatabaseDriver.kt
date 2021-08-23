package com.trollingcont.fullerene.server.repository

import com.trollingcont.fullerene.server.errorhandling.UserNotFoundException
import com.trollingcont.fullerene.server.model.RegisteredUser
import com.trollingcont.fullerene.server.model.UserBody
import com.trollingcont.fullerene.server.model.UserMap
import com.trollingcont.fullerene.server.model.Users
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.BatchUpdateStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

class UserDatabaseDriver(
    private val db: Database
) {
    init {
        transaction(db) {
            SchemaUtils.create(Users)
        }
    }

    fun getNextAutoIncrement(): Int {
        var autoIncrementId = -1

        transaction(db) {
            exec("SELECT AUTO_INCREMENT FROM information_schema.TABLES " +
                    "WHERE TABLE_SCHEMA = \"${db.name}\" AND TABLE_NAME = \"${Users.tableName}\"") { rs ->
                rs.first()
                autoIncrementId = rs.getInt("AUTO_INCREMENT")
            }
        }

        return autoIncrementId
    }

    fun addUsers(usersList: HashMap<String, UserMap>) {
        val list = usersList.map {
            RegisteredUser(it.value.id, it.key, it.value.passwordHash, it.value.salt)
        }

        transaction(db) {
            Users.batchInsertOnDuplicateKeyUpdate(list, listOf(Users.passwordHash, Users.salt)) { batch, newUser ->
                batch[id] = newUser.id
                batch[name] = newUser.name
                batch[passwordHash] = newUser.passwordHash
                batch[salt] = newUser.salt
            }
        }
    }

    fun addSingleUser(user: UserBody) {
        transaction(db) {
            Users.insert {
                it[name] = user.name
                it[passwordHash] = user.passwordHash
                it[salt] = user.salt
            }
        }
    }

    fun getUserByName(name: String) =
        transaction(db) {
            val query = Users.select {
                (Users.name eq name)
            }

            fetchUserFromQuery(query)
        }

    fun getUsersList(): HashMap<String, UserMap> {
        val usersMap = HashMap<String, UserMap>()

        transaction(db) {
            Users.selectAll().associateByTo(
                usersMap,
                { it[Users.name] },
                { UserMap(it[Users.id].value, it[Users.passwordHash], it[Users.salt]) }
            )
        }

        return usersMap
    }

    private fun fetchUserFromQuery(query: Query): RegisteredUser {
        if (query.count() == 0L) {
            throw UserNotFoundException()
        }

        return query.map {
            RegisteredUser(
                it[Users.id].value,
                it[Users.name],
                it[Users.passwordHash],
                it[Users.salt]
            )
        }[0]
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