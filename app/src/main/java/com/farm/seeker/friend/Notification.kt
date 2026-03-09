package com.farm.seeker.friend

enum class NotificationType {
    FRIEND_REQUEST,
    SYSTEM_ANNOUNCEMENT
}

data class Notification(
    val id: String,
    val title: String,
    val content: String,
    val time: String,
    val type: NotificationType,
    var isProcessed: Boolean = false, // For friend requests (accepted/declined)
    val friendId: String? = null, // For friend requests (wallet address)
    val senderUserId: String? = null,
    val senderNickname: String? = null,
    val senderAvatarIndex: Int = 1
)