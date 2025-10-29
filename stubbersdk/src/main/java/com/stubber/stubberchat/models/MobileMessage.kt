package com.stubber.stubbersdk.stubberchat.models

import org.json.JSONObject

data class MobileMessage(
    val type: String,
    val data: String,
    val attachments: List<Attachment>
) {
    companion object {
        fun fromJson(json: JSONObject): MobileMessage {
            val attachmentsList = mutableListOf<Attachment>()
            val attachmentsArray = json.optJSONArray("attachments")

            if (attachmentsArray != null) {
                for (i in 0 until attachmentsArray.length()) {
                    val attachmentObj = attachmentsArray.optJSONObject(i)
                    if (attachmentObj != null) {
                        attachmentsList.add(Attachment.fromJson(attachmentObj))
                    }
                }
            }

            return MobileMessage(
                type = json.optString("type", "text"),
                data = json.optString("data", ""),
                attachments = attachmentsList
            )
        }
    }
}
