package com.trollingcont.fullerene.server.repository.manager

import com.trollingcont.fullerene.server.errorhandling.ChatIdException
import com.trollingcont.fullerene.server.errorhandling.MessageFormatException
import com.trollingcont.fullerene.server.errorhandling.MessageIdException
import com.trollingcont.fullerene.server.errorhandling.MessageNotFoundException
import com.trollingcont.fullerene.server.model.PostedMessage
import com.trollingcont.fullerene.server.model.PostedMessageBody
import com.trollingcont.fullerene.server.repository.BufferedIO
import com.trollingcont.fullerene.server.repository.adapter.MessageDatabaseAdapter
import org.jetbrains.exposed.sql.Database
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

class MessageManager(
    db: Database
) : BufferedIO {

    private val dbAdapter = MessageDatabaseAdapter(db)
    // <messageId, PostedMessageBody>
    private val writeBuffer = HashMap<Long, PostedMessageBody>()
    // <messageId, PostedMessageBody>
    private val readBuffer = HashMap<Long, PostedMessageBody>()
    // messageId
    private val invalidIds = mutableListOf<Long>()
    // Pair<chatId, chatIndex>
    private val invalidChatIndexes = mutableListOf<Pair<Long, Long>>()
    // <chatId, <chatIndex, messageId>>
    private val messagesByChats = HashMap<Long, HashMap<Long, Long>>()
    // <chatId, messagesCount>
    private val chatMessagesCount = HashMap<Long, Long>()
    private var nextMessageId = dbAdapter.getNextMessageId()


    fun addMessage(chatId: Long, creatorUsername: String, content: String): PostedMessage {
        val errorCode = validateMessage(chatId, creatorUsername, content)

        if (errorCode != MessageErrors.NO_ERROR) {
            throw MessageFormatException(errorCode)
        }

        val nextChatIndex = 1 + (chatMessagesCount[chatId] ?: dbAdapter.getChatMessagesCount(chatId))

        val postedMessage = PostedMessage(
            nextMessageId,
            PostedMessageBody(
                LocalDateTime.now(),
                creatorUsername,
                chatId,
                nextChatIndex,
                content,
                false
            )
        )

        addMessageToBuffer(writeBuffer, postedMessage)

        chatMessagesCount[chatId] = nextChatIndex
        invalidIds.remove(nextMessageId)
        invalidChatIndexes.remove(Pair(chatId, nextChatIndex))
        nextMessageId++

        return postedMessage
    }

    fun getMessageById(messageId: Long) =
        loadMessageById(messageId)

    fun getMessageByChatIndex(chatId: Long, chatIndex: Long): PostedMessage {
        if (invalidChatIndexes.contains(Pair(chatId, chatIndex))) {
            throw MessageNotFoundException()
        }

        var postedMessage = getBufferedMessageByChatIndex(chatId, chatIndex)

        try {
            if (postedMessage == null) {
                postedMessage = dbAdapter.getMessageByChatIndex(chatId, chatIndex)
                addMessageToBuffer(readBuffer, postedMessage)
            }
        }
        catch (mnf: MessageNotFoundException) {
            invalidChatIndexes.add(Pair(chatId, chatIndex))
            throw mnf
        }

        return postedMessage
    }

    fun markMessageAsRead(messageId: Long): Boolean {
        val messageBody = loadMessageById(messageId)

        if (messageBody.isRead) {
            return false
        }

        writeBuffer[messageId] = PostedMessageBody(
            messageBody.timePosted,
            messageBody.sourceUser,
            messageBody.chatId,
            messageBody.chatIndex,
            messageBody.content,
            true
        )

        readBuffer.remove(messageId)

        return true
    }

    fun getChatMessages(chatId: Long, startPoint: Long = SELECT_ALL, count: Int = 1): List<PostedMessage> {
        val chatMessagesCount = getChatMessagesCount(chatId)

        if (startPoint > chatMessagesCount) {
            return emptyList()
        }

        val range = when (startPoint) {
            SELECT_ALL -> {
                1..chatMessagesCount
            }

            SELECT_LAST -> {
                (chatMessagesCount - count + 1)..chatMessagesCount
            }

            else -> {
                if (startPoint + count - 1 <= chatMessagesCount) {
                    startPoint until startPoint + count
                }
                else {
                    startPoint..chatMessagesCount
                }
            }
        }

        val messagesList = mutableListOf<PostedMessage>()
        val nonCachedRanges = mutableListOf<LongRange>()

        var rangeStart: Long = -1L

        for (chatIndex in range) {
            val bufferedMessage = getBufferedMessageByChatIndex(chatId, chatIndex)

            if (bufferedMessage != null) {
                messagesList.add(bufferedMessage)

                if (rangeStart != -1L) {
                    nonCachedRanges.add(rangeStart until chatIndex)
                    rangeStart = -1L
                }
            }
            else if (rangeStart == -1L) {
                rangeStart = chatIndex
            }
        }

        if (rangeStart != -1L) {
            nonCachedRanges.add(rangeStart..range.last)
        }

        for (nonCachedRange in nonCachedRanges) {
            val dbMessagesList = dbAdapter.getChatMessagesInRange(chatId, nonCachedRange)

            for (message in dbMessagesList) {
                messagesList.add(message)
                addMessageToBuffer(readBuffer, message)
            }
        }

        return messagesList
    }

    fun getChatMessagesCount(chatId: Long): Long {
        if (!isValidChatId(chatId)) {
            throw ChatIdException()
        }

        var count = chatMessagesCount[chatId]

        if (count == null) {
            count = dbAdapter.getChatMessagesCount(chatId)
            chatMessagesCount[chatId] = count
        }

        return count
    }

    override fun flushInputBuffer() {
        dbAdapter.addMessagesList(writeBuffer.map {
            PostedMessage(it.key, it.value)
        })

        readBuffer.putAll(writeBuffer)
        writeBuffer.clear()
    }

    private fun loadMessageById(messageId: Long): PostedMessageBody {
        if (!isValidMessageId(messageId)) {
            throw MessageIdException()
        }

        if (invalidIds.contains(messageId)) {
            throw MessageNotFoundException()
        }

        var messageBody = getBufferedMessageById(messageId)

        try {
            if (messageBody == null) {
                messageBody = dbAdapter.getMessageById(messageId)

                addMessageToBuffer(
                    readBuffer,
                    PostedMessage(messageId, messageBody)
                )
            }
        }
        catch (mnf: MessageNotFoundException) {
            invalidIds.add(messageId)
            throw mnf
        }

        return messageBody
    }

    private fun addMessageToBuffer(buffer: HashMap<Long, PostedMessageBody>, postedMessage: PostedMessage) {
        buffer[postedMessage.id] = postedMessage.body

        var chatMessagesList = messagesByChats[postedMessage.body.chatId]

        if (chatMessagesList == null) {
            chatMessagesList = HashMap()
            messagesByChats[postedMessage.body.chatId] = chatMessagesList
        }

        chatMessagesList[postedMessage.body.chatIndex] = postedMessage.id
    }

    private fun getBufferedMessageById(messageId: Long) =
        writeBuffer[messageId] ?: readBuffer[messageId]

    private fun getBufferedMessageByChatIndex(chatId: Long, chatIndex: Long): PostedMessage? {
        val chatMessagesList = messagesByChats[chatId] ?: return null

        val messageId = chatMessagesList[chatIndex] ?: return null

        return PostedMessage(messageId, getBufferedMessageById(messageId)!!)
    }

    private fun isMessageByChatIndexBuffered(chatId: Long, chatIndex: Long): Boolean {
        val chatMessagesList = messagesByChats[chatId] ?: return false

        return chatMessagesList[chatIndex] != null
    }

    companion object {
        const val SELECT_LAST = -1L
        const val SELECT_ALL = -2L

        fun isValidMessageId(messageId: Long) =
            messageId > 0

        fun isValidChatId(chatId: Long) =
            chatId > 0

        fun validateMessage(chatId: Long, creatorUsername: String, content: String): MessageErrors =
            when {
                chatId <= 0 -> MessageErrors.INVALID_CHAT_ID
                creatorUsername.isBlank() -> MessageErrors.CREATOR_USERNAME_EMPTY
                content.isBlank() -> MessageErrors.CONTENT_EMPTY
                else -> MessageErrors.NO_ERROR
            }
    }

    enum class MessageErrors(val errorCode: Long) {
        NO_ERROR(0),
        INVALID_CHAT_ID(1),
        CREATOR_USERNAME_EMPTY(2),
        CONTENT_EMPTY(3)
    }
}
