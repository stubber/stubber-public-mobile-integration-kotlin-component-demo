package com.stubber.stubbersdk.stubberchat.models

import org.json.JSONObject

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

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("filename", filename)
            put("originalname", originalname)
            put("fileuuid", fileuuid)
            put("contentType", contentType)
        }
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

        fun fromJson(json: JSONObject): Attachment {
            return Attachment(
                filename = json.optString("filename", ""),
                originalname = json.optString("originalname", ""),
                fileuuid = json.optString("fileuuid", ""),
                contentType = json.optString("contentType", "")
            )
        }
    }
}
