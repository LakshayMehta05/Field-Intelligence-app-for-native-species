package com.geg.fieldintel.data.model

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false
) {
    enum class Role { USER, ASSISTANT }
}
