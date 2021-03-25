package com.example.demo.controller

import com.example.demo.controller.model.*
import com.example.demo.domain.MainInteractor
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class MainController(private val mainInteractor: MainInteractor) {

    @GetMapping("/user/conversations")
    fun conversations(): ConversationsResponse {
        return ConversationsResponse(
                mainInteractor.getUserConversations(SecurityContextHolder.getContext().authentication.principal as String)
        )
    }

    @GetMapping("/user/messages")
    fun messages(
            @RequestParam(name = "page") page: Int,
            @RequestParam(name = "conversation_id") conversationId: Long
    ): MessagesResponse {
        val principalName = SecurityContextHolder.getContext().authentication.principal as String
        return mainInteractor.getMessages(principalName, conversationId, page)
    }

    @PostMapping("/user/create_conversation")
    fun createConversation(
            @RequestParam(name = "user_name") userName: String,
            @RequestParam(name = "message") message: String,
            @RequestParam(name = "conversation_title") conversationTitle: String?
    ): NewMessageResponse {
        val principalName = SecurityContextHolder.getContext().authentication.principal as String
        return NewMessageResponse(
                mainInteractor.createConversation(principalName, userName, message, conversationTitle)
        )
    }

    @PostMapping("/user/create_message")
    fun createMessage(
            @RequestParam(name = "message") message: String,
            @RequestParam(name = "conversation_id") conversationId: Long
    ): NewMessageResponse {
        val principalName = SecurityContextHolder.getContext().authentication.principal as String
        return NewMessageResponse(
                mainInteractor.createMessage(principalName, message, conversationId)
        )
    }
}