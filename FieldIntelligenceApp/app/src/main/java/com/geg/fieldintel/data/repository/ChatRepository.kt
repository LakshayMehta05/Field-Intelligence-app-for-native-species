package com.geg.fieldintel.data.repository

import com.geg.fieldintel.data.remote.BotanicalGuideApi
import com.geg.fieldintel.data.remote.ChatRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository(private val api: BotanicalGuideApi) {

    private var conversationId: String? = null

    suspend fun sendMessage(message: String, speciesId: String?): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.sendChatMessage(
                    ChatRequest(message = message, speciesId = speciesId, conversationId = conversationId)
                )
                conversationId = response.conversationId ?: conversationId
                Result.success(response.reply)
            } catch (e: Exception) {
                Result.success(offlineFallbackReply(message))
            }
        }

    /** Keeps the chat usable for a live demo even if the backend teammate's API is down. */
    private fun offlineFallbackReply(message: String): String {
        return "I couldn't reach the AI Botanical Guide service right now, but generally: " +
            "for questions like \"$message\", I'd normally look at the scanned species' family, " +
            "native region and conservation status to give you a grounded answer. " +
            "Check that BOTANICAL_API_BASE_URL is set to your backend teammate's live endpoint."
    }
}
