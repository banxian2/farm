package com.farm.seeker.friend

enum class MessageType {
    TEXT, SEED, COIN
}

data class Message(
    val id: String,
    val senderId: String,
    val content: String, // For text: message body; For seed: seed name; For coin: amount string
    val type: MessageType,
    val isMe: Boolean,
    val timestamp: Long
)
