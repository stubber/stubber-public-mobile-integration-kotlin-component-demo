package com.stubber.stubbersdk.stubberchat.models

import org.json.JSONArray
import org.json.JSONObject

data class OutgoingPayload(
    val connectionParams: Map<String, Any>,
    val message: String,
    val data: String,
    val type: String,
    val attachments: List<Attachment>
) {
    fun toJson(): JSONObject {
        val json = JSONObject()

        // Add connection params
        connectionParams.forEach { (key, value) ->
            json.put(key, value)
        }

        // Add message data
        json.put("message", message)
        json.put("data", data)
        json.put("type", type)

        // Add attachments
        val attachmentsArray = JSONArray()
        attachments.forEach { attachment ->
            attachmentsArray.put(attachment.toJson())
        }
        json.put("attachments", attachmentsArray)

        return json
    }

    companion object {
        fun create(
            connectionParams: Map<String, Any>,
            message: String,
            attachments: List<Attachment> = emptyList()
        ): OutgoingPayload {
            return OutgoingPayload(
                connectionParams = connectionParams,
                message = message,
                data = message,
                type = "text",
                attachments = attachments
            )
        }
    }
}
