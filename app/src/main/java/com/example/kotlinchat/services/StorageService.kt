package com.example.kotlinchat.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.kotlinchat.models.Message
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StorageService(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("stubber_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val MESSAGES_KEY = "chat_messages"
        private const val SESSION_UUID_KEY = "session_uuid"
        private const val PROFILE_CODE_KEY = "profile_code"
        private const val TAG = "StorageService"
    }

    // Message persistence
    fun saveMessages(messages: List<Message>) {
        try {
            val messagesJson = gson.toJson(messages.map { it.toMap() })
            prefs.edit().putString(MESSAGES_KEY, messagesJson).apply()
            Log.d(TAG, "Saved ${messages.size} messages to storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving messages", e)
        }
    }

    fun loadMessages(): List<Message> {
        return try {
            val messagesString = prefs.getString(MESSAGES_KEY, null)
            if (messagesString.isNullOrEmpty()) {
                Log.d(TAG, "No saved messages found")
                emptyList()
            } else {
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                val messagesList: List<Map<String, Any>> = gson.fromJson(messagesString, type)
                val messages = messagesList.map { Message.fromMap(it) }
                Log.d(TAG, "Loaded ${messages.size} messages from storage")
                messages
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading messages", e)
            emptyList()
        }
    }

    fun clearMessages() {
        try {
            prefs.edit().remove(MESSAGES_KEY).apply()
            Log.d(TAG, "Cleared messages from storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing messages", e)
        }
    }

    // Session UUID persistence
    fun saveSessionUuid(sessionUuid: String?) {
        try {
            if (sessionUuid == null) {
                prefs.edit().remove(SESSION_UUID_KEY).apply()
                Log.d(TAG, "Removed session UUID from storage")
            } else {
                prefs.edit().putString(SESSION_UUID_KEY, sessionUuid).apply()
                Log.d(TAG, "Saved session UUID to storage: $sessionUuid")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving session UUID", e)
        }
    }

    fun loadSessionUuid(): String? {
        return try {
            val sessionUuid = prefs.getString(SESSION_UUID_KEY, null)
            if (sessionUuid != null) {
                Log.d(TAG, "Loaded session UUID from storage: $sessionUuid")
            } else {
                Log.d(TAG, "No session UUID found in storage")
            }
            sessionUuid
        } catch (e: Exception) {
            Log.e(TAG, "Error loading session UUID", e)
            null
        }
    }

    fun clearSessionUuid() {
        try {
            prefs.edit().remove(SESSION_UUID_KEY).apply()
            Log.d(TAG, "Cleared session UUID from storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing session UUID", e)
        }
    }

    // Profile code persistence
    fun saveProfileCode(profileCode: String?) {
        try {
            if (profileCode == null) {
                prefs.edit().remove(PROFILE_CODE_KEY).apply()
                Log.d(TAG, "Removed profile code from storage")
            } else {
                prefs.edit().putString(PROFILE_CODE_KEY, profileCode).apply()
                Log.d(TAG, "Saved profile code to storage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving profile code", e)
        }
    }

    fun loadProfileCode(): String? {
        return try {
            val profileCode = prefs.getString(PROFILE_CODE_KEY, null)
            if (profileCode != null) {
                Log.d(TAG, "Loaded profile code from storage")
            } else {
                Log.d(TAG, "No profile code found in storage")
            }
            profileCode
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profile code", e)
            null
        }
    }

    // Get all connection parameters as a map
    fun getConnectionParams(): Map<String, String> {
        return try {
            val params = mutableMapOf<String, String>()

            // Get profile code
            val profileCode = prefs.getString(PROFILE_CODE_KEY, null)
            if (!profileCode.isNullOrEmpty()) {
                params["profile_code"] = profileCode
            } else {
                params["profile_code"] = "ROTOBP" // Default fallback
            }

            // Get session UUID if it exists
            val sessionUuid = prefs.getString(SESSION_UUID_KEY, null)
            if (!sessionUuid.isNullOrEmpty()) {
                params["sessionuuid"] = sessionUuid
            }

            Log.d(TAG, "Retrieved connection params from storage: $params")
            params
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connection params", e)
            mapOf("profile_code" to "ROTOBP") // Fallback
        }
    }

    // Clear all stored data
    fun clearAll() {
        try {
            prefs.edit()
                .remove(MESSAGES_KEY)
                .remove(SESSION_UUID_KEY)
                .remove(PROFILE_CODE_KEY)
                .apply()
            Log.d(TAG, "Cleared all data from storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all data", e)
        }
    }
}
