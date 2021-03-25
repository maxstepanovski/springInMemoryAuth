package com.example.demo.domain

import com.example.demo.controller.model.ConversationResponse
import com.example.demo.controller.model.MessageResponse
import com.example.demo.controller.model.MessagesResponse
import com.example.demo.controller.model.UserResponse
import com.example.demo.data.*
import com.example.demo.data.model.*
import org.springframework.data.domain.PageRequest

class MainInteractor(
        private val conversationRepository: ConversationRepository,
        private val userConversationRepository: UserConversationRepository,
        private val messageRepository: MessageRepository,
        private val conversationMessageRepository: ConversationMessageRepository,
        private val userRepository: UserRepository
) {

    fun getSelf(principalName: String): UserResponse {
        val user = userRepository.findByUserName(principalName)
        return UserResponse(
                user.id,
                user.userName
        )
    }

    fun getUserConversations(userName: String): List<ConversationResponse> {
        val user = userRepository.findByUserName(userName)
        val conversationIds = userConversationRepository.findAllByUserId(user.id).map { it.conversationId }
        return conversationRepository.findAllById(conversationIds).map { ConversationResponse(it.id, it.name) }
    }

    fun getConversationUsers(conversationId: Long): List<UserResponse> {
        val userIds = userConversationRepository.findAllByConversationId(conversationId).map { it.userId }
        val users = userRepository.findAllById(userIds)
        return users.map { UserResponse(it.id, it.userName) }
    }

    fun getMessages(conversationId: Long, page: Int): MessagesResponse {
        val messageIdsPage = conversationMessageRepository.findAllByConversationId(conversationId, PageRequest.of(page, 10))
        val messageIds = messageIdsPage.content.map { it.messageId }
        val messages = messageRepository.findAllById(messageIds)
                .map { MessageResponse(it.id, it.text, it.time, it.userId) }
        return MessagesResponse(
                messages,
                page,
                messageIdsPage.totalPages > page + 1
        )
    }

    fun createConversation(principalName: String, userName: String, messageText: String, conversationTitle: String?): Boolean {
        val conversation = conversationRepository.save(ConversationEntity(0, conversationTitle.orEmpty()))
        val user = userRepository.findByUserName(userName)
        val principal = userRepository.findByUserName(principalName)

        userConversationRepository.save(UserConversationEntity(0, user.id, conversation.id))
        userConversationRepository.save(UserConversationEntity(0, principal.id, conversation.id))
        val message = messageRepository.save(MessageEntity(0, messageText, System.currentTimeMillis(), principal.id))
        conversationMessageRepository.save(ConversationMessageEntity(0, conversation.id, message.id))
        return true
    }

    fun createMessage(principalName: String, messageText: String, conversationId: Long): Boolean {
        val principal = userRepository.findByUserName(principalName)
        val message = messageRepository.save(MessageEntity(0, messageText, System.currentTimeMillis(), principal.id))
        conversationMessageRepository.save(ConversationMessageEntity(0, conversationId, message.id))
        return true
    }
}