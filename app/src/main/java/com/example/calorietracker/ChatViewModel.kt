package com.example.calorietracker

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.data.DataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: DataRepository,
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager
) : ViewModel() {

    private var _messages by mutableStateOf<List<ChatMessage>>(emptyList())
    var messages: List<ChatMessage>
        get() = _messages
        private set(value) { _messages = value }
    fun setMessages(value: List<ChatMessage>) { _messages = value }

    var inputMessage by mutableStateOf("")
        private set
    fun setInputMessage(value: String) { inputMessage = value }
}
