package com.geg.fieldintel.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geg.fieldintel.data.model.ChatMessage
import com.geg.fieldintel.data.remote.RetrofitClient
import com.geg.fieldintel.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository(RetrofitClient.botanicalGuideApi),
    /** Optional: grounds the chat in the last species scanned in AR, e.g. "sp1". */
    private var contextSpeciesId: String? = null
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = ChatMessage.Role.ASSISTANT,
                text = "Hi! I'm your AI Botanical Guide. Scan a plant or ask me anything about " +
                    "native species, conservation status, or the ecosystem around you."
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    fun setSpeciesContext(speciesId: String?) {
        contextSpeciesId = speciesId
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(role = ChatMessage.Role.USER, text = text)
        val loadingMessage = ChatMessage(role = ChatMessage.Role.ASSISTANT, text = "", isLoading = true)
        _messages.update { it + userMessage + loadingMessage }

        viewModelScope.launch {
            val result = repository.sendMessage(text, contextSpeciesId)
            _messages.update { current ->
                current.dropLast(1) + ChatMessage(
                    role = ChatMessage.Role.ASSISTANT,
                    text = result.getOrElse { "Something went wrong. Please try again." }
                )
            }
        }
    }
}
