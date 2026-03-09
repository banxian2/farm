package com.farm.seeker.friend

enum class FriendStatus {
    FRIEND,
    REQUEST_RECEIVED,
    REQUEST_SENT,
    STRANGER
}

data class Friend(
    val id: String,
    val walletAddress: String,
    val name: String,
    val level: Int,
    val isOnline: Boolean,
    val lastSeen: String = "",
    val avatarIndex: Int = 1,
    var status: FriendStatus = FriendStatus.FRIEND
)