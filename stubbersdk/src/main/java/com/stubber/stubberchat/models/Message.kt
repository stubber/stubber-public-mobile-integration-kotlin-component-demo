package com.stubber.stubbersdk.stubberchat.models

import java.util.Date

data class Message(
    val direction: MessageDirection,
    val message: String,
    val attachments: List<Attachment> = emptyList(),
    val timestamp: Date = Date(),
    val type: String = "text"
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
            "timestamp" to timestamp.time,
            "type" to type
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
                timestamp = Date(timestampLong),
                type = (map["type"] as? String) ?: "text"
            )
        }
    }
}
