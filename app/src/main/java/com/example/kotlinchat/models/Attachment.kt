package com.example.kotlinchat.models

import com.example.kotlinchat.config.Environment

data class Attachment(
    val filename: String,
    val originalname: String,
    val fileuuid: String,
    val contentType: String
) {
    // Build URL from fileuuid
    val url: String
        get() = "${Environment.fileServerUrl}/$fileuuid"

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
                filename = map["filename"] as String,
                originalname = map["originalname"] as String,
                fileuuid = map["fileuuid"] as String,
                contentType = map["contentType"] as String
            )
        }
    }
}
