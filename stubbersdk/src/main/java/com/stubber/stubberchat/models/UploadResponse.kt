package com.stubber.stubbersdk.stubberchat.models

import org.json.JSONObject

data class UploadResponse(
    val files: List<Attachment>?,
    val attachments: List<Attachment>?
) {
    fun getAllAttachments(): List<Attachment> {
        return files ?: attachments ?: emptyList()
    }

    companion object {
        fun fromJson(json: JSONObject): UploadResponse {
            val filesList = mutableListOf<Attachment>()
            val attachmentsList = mutableListOf<Attachment>()

            // Parse "files" array
            json.optJSONArray("files")?.let { filesArray ->
                for (i in 0 until filesArray.length()) {
                    filesArray.optJSONObject(i)?.let { fileObj ->
                        filesList.add(Attachment.fromJson(fileObj))
                    }
                }
            }

            // Parse "attachments" array
            json.optJSONArray("attachments")?.let { attachmentsArray ->
                for (i in 0 until attachmentsArray.length()) {
                    attachmentsArray.optJSONObject(i)?.let { attachmentObj ->
                        attachmentsList.add(Attachment.fromJson(attachmentObj))
                    }
                }
            }

            return UploadResponse(
                files = if (filesList.isNotEmpty()) filesList else null,
                attachments = if (attachmentsList.isNotEmpty()) attachmentsList else null
            )
        }
    }
}
