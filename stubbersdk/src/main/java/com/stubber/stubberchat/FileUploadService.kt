package com.stubber.stubbersdk.stubberchat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.stubber.stubbersdk.stubberchat.models.Attachment
import com.stubber.stubbersdk.stubberchat.models.UploadResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class FileUploadService(
    private val context: Context,
    private val config: ChatConfig
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "FileUploadService"
    }

    suspend fun uploadFilesDirectly(files: List<File>): List<Attachment> = withContext(Dispatchers.IO) {
        val attachments = mutableListOf<Attachment>()

        try {
            val url = "${config.serverUrl}/v2/attachments?profile_code=${config.profileCode}"
            Log.d(TAG, "Uploading to: $url")

            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)

            // Add each file to the multipart request
            files.forEach { file ->
                if (file.exists() && file.length() > 0) {
                    // Determine mime type from file extension
                    val mimeType = when (file.extension.lowercase()) {
                        "m4a" -> "audio/mp4"
                        "mp3" -> "audio/mpeg"
                        "aac" -> "audio/aac"
                        "jpg", "jpeg" -> "image/jpeg"
                        "png" -> "image/png"
                        else -> "application/octet-stream"
                    }

                    Log.d(TAG, "Preparing to upload file: ${file.name}, size: ${file.length()} bytes, type: $mimeType")
                    val requestBody = file.asRequestBody(mimeType.toMediaType())
                    multipartBuilder.addFormDataPart(
                        "files",
                        file.name,
                        requestBody
                    )
                    Log.d(TAG, "Added file: ${file.name}, type: $mimeType, size: ${file.length()}")
                } else {
                    Log.e(TAG, "File doesn't exist or is empty: ${file.absolutePath}")
                }
            }

            val requestBody = multipartBuilder.build()
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "Upload response code: ${response.code}")
            Log.d(TAG, "Upload response body: $responseBody")

            if (response.isSuccessful && responseBody != null) {
                // Parse the response using UploadResponse model
                try {
                    val jsonResponse = JSONObject(responseBody)
                    val uploadResponse = UploadResponse.fromJson(jsonResponse)
                    attachments.addAll(uploadResponse.getAllAttachments())
                    Log.d(TAG, "Successfully parsed ${attachments.size} attachments")
                } catch (e: Exception) {
                    // Try parsing as direct array
                    try {
                        val filesArray = JSONArray(responseBody)
                        for (i in 0 until filesArray.length()) {
                            filesArray.optJSONObject(i)?.let { fileObj ->
                                attachments.add(Attachment.fromJson(fileObj))
                            }
                        }
                        Log.d(TAG, "Successfully parsed ${attachments.size} attachments from array")
                    } catch (e2: Exception) {
                        Log.e(TAG, "Failed to parse response: ${e.message}, ${e2.message}")
                    }
                }
            } else {
                Log.e(TAG, "Upload failed with code: ${response.code}, body: $responseBody")
            }

            response.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading files", e)
        }

        return@withContext attachments
    }

    suspend fun uploadFiles(uris: List<Uri>): List<Attachment> = withContext(Dispatchers.IO) {
        val attachments = mutableListOf<Attachment>()

        try {
            val url = "${config.serverUrl}/v2/attachments?profile_code=${config.profileCode}"
            Log.d(TAG, "Uploading to: $url")

            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)

            // Add each file to the multipart request
            uris.forEach { uri ->
                val file = uriToFile(uri)
                if (file != null) {
                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    Log.d(TAG, "Preparing to upload file: ${file.name}, size: ${file.length()} bytes, type: $mimeType")
                    val requestBody = file.asRequestBody(mimeType.toMediaType())
                    multipartBuilder.addFormDataPart(
                        "files",
                        file.name,
                        requestBody
                    )
                    Log.d(TAG, "Added file: ${file.name}, type: $mimeType")
                } else {
                    Log.e(TAG, "Failed to convert URI to file: $uri")
                }
            }

            val requestBody = multipartBuilder.build()
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "Upload response code: ${response.code}")
            Log.d(TAG, "Upload response body: $responseBody")

            if (response.isSuccessful && responseBody != null) {
                // Parse the response using UploadResponse model
                try {
                    val jsonResponse = JSONObject(responseBody)
                    val uploadResponse = UploadResponse.fromJson(jsonResponse)
                    attachments.addAll(uploadResponse.getAllAttachments())
                    Log.d(TAG, "Successfully parsed ${attachments.size} attachments")
                } catch (e: Exception) {
                    // Try parsing as direct array
                    try {
                        val filesArray = JSONArray(responseBody)
                        for (i in 0 until filesArray.length()) {
                            filesArray.optJSONObject(i)?.let { fileObj ->
                                attachments.add(Attachment.fromJson(fileObj))
                            }
                        }
                        Log.d(TAG, "Successfully parsed ${attachments.size} attachments from array")
                    } catch (e2: Exception) {
                        Log.e(TAG, "Failed to parse response: ${e.message}, ${e2.message}")
                    }
                }
            } else {
                Log.e(TAG, "Upload failed with code: ${response.code}, body: $responseBody")
            }

            response.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading files", e)
        }

        return@withContext attachments
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val fileName = getFileName(uri) ?: "temp_${System.currentTimeMillis()}"
            val tempFile = File(context.cacheDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to file", e)
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null

        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex)
                    }
                }
            }
        }

        if (fileName == null) {
            fileName = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }

        return fileName
    }
}