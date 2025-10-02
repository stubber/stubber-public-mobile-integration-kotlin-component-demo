package com.example.kotlinchat.models

import java.util.Date

enum class MessageDirection {
    INCOMING, OUTGOING
}

data class UploadingAttachment(
    val filename: String,
    val filePath: String,
    val isUploading: Boolean = true,
    val uploadedUrl: String? = null
)

data class Message(
    val direction: MessageDirection,
    val message: String,
    val attachments: List<Attachment> = emptyList(),
    val uploadingAttachments: List<UploadingAttachment> = emptyList(),
    val timestamp: Date = Date()
) {
    val isIncoming: Boolean
        get() = direction == MessageDirection.INCOMING

    val isOutgoing: Boolean
        get() = direction == MessageDirection.OUTGOING

    fun toMap(): Map<String, Any> {
        return mapOf(
            "direction" to direction.name,
            "message" to message,
            "attachments" to attachments.map { it.toMap() },
            "timestamp" to timestamp.time
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): Message {
            return Message(
                direction = MessageDirection.valueOf(map["direction"] as String),
                message = map["message"] as String,
                attachments = (map["attachments"] as? List<Map<String, Any>>)
                    ?.map { Attachment.fromMap(it) } ?: emptyList(),
                timestamp = Date(map["timestamp"] as Long)
            )
        }
    }
}
