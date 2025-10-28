package com.stubber.stubbersdk.stubberchat

import java.util.Date

enum class MessageDirection {
    INCOMING, OUTGOING
}

data class Message(
    val direction: MessageDirection,
    val message: String,
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

            return Message(
                direction = MessageDirection.valueOf(map["direction"] as String),
                message = map["message"] as String,
                timestamp = Date(timestampLong)
            )
        }
    }
}
