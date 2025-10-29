package com.stubber.stubbersdk.stubberchat

import java.util.Date

enum class MessageDirection {
    INCOMING, OUTGOING
}

data class Attachment(
    val filename: String,
    val originalname: String,
    val fileuuid: String,
    val contentType: String
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "filename" to filename,
            "originalname" to originalname,
            "fileuuid" to fileuuid,
            "contentType" to contentType
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): Attachment {
            return Attachment(
                filename = map["filename"] as? String ?: "",
                originalname = map["originalname"] as? String ?: "",
                fileuuid = map["fileuuid"] as? String ?: "",
                contentType = map["contentType"] as? String ?: ""
            )
        }
    }
}

data class Message(
    val direction: MessageDirection,
    val message: String,
    val attachments: List<Attachment> = emptyList(),
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
            val timestampValue = map["timestamp"]
            val timestampLong = when (timestampValue) {
                is Long -> timestampValue
                is Double -> timestampValue.toLong()
                is Int -> timestampValue.toLong()
                else -> Date().time
            }

            val attachmentsList = (map["attachments"] as? List<*>)?.mapNotNull { attachmentMap ->
                (attachmentMap as? Map<*, *>)?.let {
                    @Suppress("UNCHECKED_CAST")
                    Attachment.fromMap(it as Map<String, Any>)
                }
            } ?: emptyList()

            return Message(
                direction = MessageDirection.valueOf(map["direction"] as String),
                message = map["message"] as String,
                attachments = attachmentsList,
                timestamp = Date(timestampLong)
            )
        }
    }
}
