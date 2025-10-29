package com.stubber.stubbersdk.stubberchat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class MessageViewModel(
    application: Application,
    private val chatService: ChatService
) : AndroidViewModel(application) {

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    companion object {
        private const val TAG = "MessageViewModel"
    }

    init {
        loadMessages()
    }

    fun addMessage(message: Message) {
        val currentMessages = _messages.value.orEmpty().toMutableList()
        currentMessages.add(message)
        _messages.value = currentMessages
        saveMessages()
    }

    fun addIncomingMessage(payload: JSONObject) {
        val messageText = payload.optString("data", "")

        // Parse attachments
        val attachments = mutableListOf<Attachment>()
        val attachmentsArray = payload.optJSONArray("attachments")
        if (attachmentsArray != null) {
            for (i in 0 until attachmentsArray.length()) {
                val attachmentObj = attachmentsArray.optJSONObject(i)
                if (attachmentObj != null) {
                    val attachment = Attachment(
                        filename = attachmentObj.optString("filename", ""),
                        originalname = attachmentObj.optString("originalname", ""),
                        fileuuid = attachmentObj.optString("fileuuid", ""),
                        contentType = attachmentObj.optString("contentType", "")
                    )
                    attachments.add(attachment)
                }
            }
        }

        if (messageText.isNotEmpty() || attachments.isNotEmpty()) {
            val message = Message(
                direction = MessageDirection.INCOMING,
                message = messageText,
                attachments = attachments
            )
            addMessage(message)
        }
    }

    fun addOutgoingMessage(messageText: String) {
        val message = Message(
            direction = MessageDirection.OUTGOING,
            message = messageText
        )
        addMessage(message)
    }

    fun clearMessages() {
        _messages.value = emptyList()
        saveMessages()
    }

    fun loadMessages() {
        viewModelScope.launch {
            try {
                val savedMessages = chatService.loadMessages()
                _messages.postValue(savedMessages)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading messages", e)
            }
        }
    }

    private fun saveMessages() {
        viewModelScope.launch {
            try {
                val messagesToSave = _messages.value.orEmpty()
                chatService.saveMessages(messagesToSave)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving messages", e)
            }
        }
    }
}
