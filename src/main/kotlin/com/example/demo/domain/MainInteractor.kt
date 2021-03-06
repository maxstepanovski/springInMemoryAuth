package com.example.demo.domain

import com.example.demo.Opened
import com.example.demo.controller.model.*
import com.example.demo.data.*
import com.example.demo.data.model.*
import com.example.demo.domain.model.PushNotification
import org.springframework.transaction.annotation.Transactional

@Opened
class MainInteractor(
    private val fcmInteractor: FcmInteractor,
    private val conversationRepository: ConversationRepository,
    private val userConversationRepository: UserConversationRepository,
    private val messageRepository: MessageRepository,
    private val conversationMessageRepository: ConversationMessageRepository,
    private val userRepository: UserRepository,
    private val userFirebaseTokenRepository: UserFirebaseTokenRepository
) {

    @Transactional
    fun isUserExists(userName: String): Boolean =
        userRepository.existsByUserName(userName)

    @Transactional
    fun getUserConversations(userName: String): List<ConversationResponse> {
        val result = mutableListOf<ConversationResponse>()

        val user = userRepository.findByUserName(userName)
        val conversationIds = userConversationRepository.findAllByUserId(user.id).map { it.conversationId }

        conversationRepository.findAllById(conversationIds).forEach {
            val lastMessageId = conversationMessageRepository.findLatestByConversationId(it.id).messageId
            val lastMessage = messageRepository.findById(lastMessageId).get()
            val messageOwner = userRepository.findById(lastMessage.userId).get()
            result.add(
                ConversationResponse(
                    it.id,
                    it.name,
                    MessageResponse(
                        lastMessage.id,
                        lastMessage.text,
                        lastMessage.time,
                        messageOwner.userName,
                        messageOwner.userName == user.userName
                    )
                )
            )
        }

        return result
    }

    @Transactional
    fun getConversationUsers(conversationId: Long): List<UserEntity> =
        userConversationRepository.findAllByConversationId(conversationId)
            .map {
                it.userId
            }
            .let {
                userRepository.findAllById(it)
            }

    @Transactional
    fun getMessages(principalName: String, conversationId: Long, pageSize: Int, lastMessageId: Long): MessagesSocketResponse {
        // find all message ids which satisfy constraints
        val messageIds = if (lastMessageId < 0) {
            conversationMessageRepository.findAllByConversationId(conversationId, pageSize)
        } else {
            conversationMessageRepository.findAllByConversationId(conversationId, lastMessageId, pageSize)
        }.map {
            it.messageId
        }

        // check if there's more messages which satisfy constraints
        val hasMore = if (messageIds.size < pageSize) {
            false
        } else {
            conversationMessageRepository.existsWithIdLessThan(conversationId, messageIds.last())
        }

        // find all users who take part in conversation
        val userIds = userConversationRepository.findAllByConversationId(conversationId).map { it.userId }
        val users = userRepository.findAllById(userIds)
        val userMap = mutableMapOf<Long, UserEntity>().apply {
            users.forEach {
                this[it.id] = it
            }
        }

        // form the final response
        val principal = userRepository.findByUserName(principalName)
        val messages = messageRepository.findAllById(messageIds)
            .map {
                MessageSocketResponse(
                    it.id,
                    it.text,
                    it.time,
                    userMap[it.userId]?.userName.orEmpty(),
                    principal.id == it.userId
                )
            }
        return MessagesSocketResponse(
            messages,
            hasMore
        )
    }

    @Transactional
    fun createConversation(
        principalName: String,
        userName: String,
        messageText: String,
        conversationTitle: String?
    ): Long {
        val conversation = conversationRepository.save(ConversationEntity(0, conversationTitle.orEmpty()))
        val user = userRepository.findByUserName(userName)
        val principal = userRepository.findByUserName(principalName)

        userConversationRepository.save(UserConversationEntity(0, user.id, conversation.id))
        userConversationRepository.save(UserConversationEntity(0, principal.id, conversation.id))
        val message = messageRepository.save(MessageEntity(0, messageText, System.currentTimeMillis(), principal.id))
        conversationMessageRepository.save(ConversationMessageEntity(0, conversation.id, message.id))
        return conversation.id
    }

    @Transactional
    fun createMessageWithNotifications(principalName: String, messageText: String, conversationId: Long): Boolean {
        val principal = userRepository.findByUserName(principalName)
        val message = messageRepository.save(MessageEntity(0, messageText, System.currentTimeMillis(), principal.id))
        conversationMessageRepository.save(ConversationMessageEntity(0, conversationId, message.id))
        userConversationRepository
            .findAllByConversationId(conversationId)
            .map { it.userId }
            .filter { it != principal.id }
            .forEach { userId ->
                userFirebaseTokenRepository
                    .findAllByUserId(userId)
                    .map { it.firebaseToken }
                    .forEach {
                        fcmInteractor.sendNotification(PushNotification(principalName, messageText), it)
                    }
            }
        return true
    }

    @Transactional
    fun createMessage(principalName: String, messageText: String, conversationId: Long): MessageEntity {
        val principal = userRepository.findByUserName(principalName)
        val message = messageRepository.save(MessageEntity(0, messageText, System.currentTimeMillis(), principal.id))
        conversationMessageRepository.save(ConversationMessageEntity(0, conversationId, message.id))
        return message
    }

    @Transactional
    fun sendNotification(principalName: String, messageText: String, conversationId: Long, userId: Long) {
        val token = userFirebaseTokenRepository.findByUserId(userId).firebaseToken
        fcmInteractor.sendNotification(PushNotification(principalName, messageText), token)
    }

    @Transactional
    fun saveFirebaseToken(principalName: String, token: String): Boolean {
        val principal = userRepository.findByUserName(principalName)
        userFirebaseTokenRepository.insertWithReplace(principal.id, token)
        return true
    }
}