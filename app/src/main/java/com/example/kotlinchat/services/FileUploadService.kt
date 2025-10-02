package com.example.kotlinchat.services

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class FileUploadService(private val context: Context) {
    private val client = OkHttpClient()
    private val gson = Gson()

    companion object {
        private const val TAG = "FileUploadService"
    }

    suspend fun uploadFile(
        fileUri: Uri,
        serverBaseUrl: String,
        profileCode: String?
    ): Map<String, Any>? {
        return try {
            Log.d(TAG, "Starting file upload: $fileUri")

            // Create a temporary file from URI
            val file = getFileFromUri(fileUri) ?: return null

            var uploadUrl = "$serverBaseUrl/v2/attachments"

            // Add profile_code as query parameter if provided
            if (!profileCode.isNullOrEmpty()) {
                uploadUrl += "?profile_code=$profileCode"
                Log.d(TAG, "Upload URL with profile_code: $uploadUrl")
            } else {
                Log.d(TAG, "No profile code provided")
            }

            // Detect the content type of the file
            val mimeType = getMimeType(fileUri) ?: "application/octet-stream"
            Log.d(TAG, "Detected MIME type: $mimeType for file: ${file.name}")

            // Create request body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody(mimeType.toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .build()

            Log.d(TAG, "Sending upload request...")

            val response = client.newCall(request).execute()

            Log.d(TAG, "Upload response status: ${response.code}")
            val responseBody = response.body?.string()
            Log.d(TAG, "Upload response body: $responseBody")

            // Clean up temporary file
            file.delete()

            if (response.isSuccessful && responseBody != null) {
                // Server returns an array of file objects, we need the first one
                val responseData = gson.fromJson(responseBody, List::class.java)
                if (responseData.isNotEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    val fileData = responseData[0] as Map<String, Any>
                    Log.d(TAG, "File uploaded successfully: $fileData")
                    return fileData
                }
            } else {
                throw Exception("Upload failed with status: ${response.code}")
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "File upload error", e)
            null
        }
    }

    suspend fun uploadMultipleFiles(
        fileUris: List<Uri>,
        serverBaseUrl: String,
        profileCode: String?
    ): List<Map<String, Any>> {
        val uploadResults = mutableListOf<Map<String, Any>>()

        for (fileUri in fileUris) {
            val result = uploadFile(fileUri, serverBaseUrl, profileCode)
            if (result != null) {
                uploadResults.add(result)
            }
        }

        return uploadResults
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = getFileName(uri)
            val tempFile = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(tempFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating file from URI", e)
            null
        }
    }

    private fun getFileName(uri: Uri): String {
        var result = "temp_file"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    result = it.getString(nameIndex)
                }
            }
        }
        return result
    }

    private fun getMimeType(uri: Uri): String? {
        return if (uri.scheme == "content") {
            context.contentResolver.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())
        }
    }
}
