package com.example.kotlinchat.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kotlinchat.models.Attachment
import com.example.kotlinchat.models.Message
import com.example.kotlinchat.models.MessageDirection
import com.example.kotlinchat.models.UploadingAttachment
import com.example.kotlinchat.services.StorageService
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MessageViewModel(application: Application) : AndroidViewModel(application) {
    private val storageService = StorageService(application)

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
        if (messageText.isNotEmpty()) {
            // Process attachments
            val attachments = mutableListOf<Attachment>()
            val attachmentsArray = payload.optJSONArray("attachments")
            if (attachmentsArray != null) {
                for (i in 0 until attachmentsArray.length()) {
                    val attachmentObj = attachmentsArray.getJSONObject(i)
                    val fileuuid = attachmentObj.optString("fileuuid", "")
                    val filename = attachmentObj.optString("filename", "")
                    val contentType = attachmentObj.optString("contentType", "")
                    val originalname = attachmentObj.optString("originalname", "")

                    if (fileuuid.isNotEmpty()) {
                        attachments.add(
                            Attachment(
                                filename = filename,
                                originalname = originalname,
                                fileuuid = fileuuid,
                                contentType = contentType
                            )
                        )
                    }
                }
            }

            val message = Message(
                direction = MessageDirection.INCOMING,
                message = messageText,
                attachments = attachments
            )

            addMessage(message)
        }
    }

    fun addOutgoingMessage(
        messageText: String,
        uploadingFiles: List<UploadingAttachment> = emptyList()
    ) {
        val message = Message(
            direction = MessageDirection.OUTGOING,
            message = messageText,
            attachments = emptyList(),
            uploadingAttachments = uploadingFiles
        )
        addMessage(message)
    }

    fun addOutgoingMessageWithAttachments(
        messageText: String,
        attachments: List<Attachment> = emptyList(),
        uploadingFiles: List<UploadingAttachment> = emptyList()
    ) {
        val message = Message(
            direction = MessageDirection.OUTGOING,
            message = messageText,
            attachments = attachments,
            uploadingAttachments = uploadingFiles
        )
        addMessage(message)
    }

    fun updateMessageWithUploadedFiles(
        message: Message,
        uploadedFiles: List<Attachment>
    ) {
        val currentMessages = _messages.value.orEmpty().toMutableList()
        val index = currentMessages.indexOf(message)
        if (index != -1) {
            // Create a new message with uploaded files and clear uploading attachments
            val updatedMessage = Message(
                direction = message.direction,
                message = message.message,
                attachments = message.attachments + uploadedFiles,
                uploadingAttachments = emptyList(), // Clear uploading attachments
                timestamp = message.timestamp
            )
            currentMessages[index] = updatedMessage
            _messages.value = currentMessages
            saveMessages()
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
        saveMessages()
    }

    fun loadMessages() {
        viewModelScope.launch {
            try {
                val savedMessages = storageService.loadMessages()
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
                storageService.saveMessages(messagesToSave)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving messages", e)
            }
        }
    }
}
