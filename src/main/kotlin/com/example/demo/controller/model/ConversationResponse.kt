package com.example.demo.controller.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ConversationResponse(
        @JsonProperty(value = "id")
        val id: Long,

        @JsonProperty(value = "name")
        val name: String,

        @JsonProperty(value = "last_message")
        val lastMessage: MessageResponse
)