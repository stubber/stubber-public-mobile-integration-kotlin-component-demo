package com.stubber.stubberchatsdk.chat

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class ChatService private constructor(
    private val context: Context,
    private val config: ChatConfig
) {

    // Socket properties
    private var socket: Socket? = null
    private var isSocketConnected = false
    private var messageCallback: ((JSONObject) -> Unit)? = null
    private var sessionUuidCallback: ((String) -> Unit)? = null

    // Storage properties
    private val prefs: SharedPreferences =
        context.getSharedPreferences("stubber_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val TAG = "ChatService"
        private const val MESSAGES_KEY = "chat_messages"
        private const val SESSION_UUID_KEY = "session_uuid"

        @Volatile
        private var instance: ChatService? = null

        fun getInstance(context: Context, config: ChatConfig): ChatService {
            return instance ?: synchronized(this) {
                instance ?: ChatService(context.applicationContext, config).also { instance = it }
            }
        }
    }

    // ==================== SOCKET METHODS ====================

    fun isConnected(): Boolean = isSocketConnected

    fun getSocket(): Socket? = socket

    fun setMessageCallback(callback: (JSONObject) -> Unit) {
        messageCallback = callback
    }

    fun setSessionUuidCallback(callback: (String) -> Unit) {
        sessionUuidCallback = callback
    }

    fun connect() {
        try {
            val serverUrl = config.serverUrl
            val opts = IO.Options().apply {
                path = "/v2/socket.io/"
                transports = arrayOf("websocket", "polling")
                reconnection = true
            }

            val newSocket = IO.socket(serverUrl, opts)
            setupSocketListeners(newSocket)
            newSocket.connect()
            socket = newSocket
            Log.d(TAG, "Socket connecting to: $serverUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing socket", e)
        }
    }

    fun disconnect() {
        socket?.let {
            it.disconnect()
            it.off()
            socket = null
            isSocketConnected = false
            Log.d(TAG, "Socket disconnected")
        }
    }

    fun emit(event: String, data: Any) {
        if (socket != null && isSocketConnected) {
            socket?.emit(event, data)
            Log.d(TAG, "Emitted event: $event with data: $data")
        } else {
            Log.w(TAG, "Socket not connected. Cannot emit event: $event")
        }
    }

    private fun setupSocketListeners(socket: Socket) {
        socket.on(Socket.EVENT_CONNECT) {
            isSocketConnected = true
            Log.d(TAG, "Socket connected")

            val query = getConnectionParams()
            val configData = JSONObject(query as Map<*, *>)
            socket.emit("config", configData)
            Log.d(TAG, ">>> Using connection params from storage: $query")
        }

        socket.on(Socket.EVENT_DISCONNECT) {
            isSocketConnected = false
            Log.d(TAG, "Socket disconnected")
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e(TAG, "Connection error: ${args.joinToString()}")
        }

        socket.on("sessionuuid") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as? JSONObject
                val sessionUuid = data?.optString("session")
                if (!sessionUuid.isNullOrEmpty()) {
                    saveSessionUuid(sessionUuid)
                    sessionUuidCallback?.invoke(sessionUuid)
                    Log.d(TAG, "Session UUID updated to: $sessionUuid")
                }
            }
        }

        socket.on("payload") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as? JSONObject
                if (data != null) {
                    Log.d(TAG, "Payload received: $data")

                    if (data.has("data") && data.optString("type") == "text") {
                        Log.d(TAG, "Incoming message: ${data.optString("data")}")
                        messageCallback?.invoke(data)
                    }
                }
            }
        }
    }

    // ==================== STORAGE METHODS ====================

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


    fun getConnectionParams(): Map<String, String> {
        return try {
            val params = mutableMapOf<String, String>()

            // Profile code and UUID come from config
            params["profileuuid"] = config.profileUuid
            params["branch"] = config.profileBranch

            // Session UUID comes from storage
            val sessionUuid = prefs.getString(SESSION_UUID_KEY, null)
            if (!sessionUuid.isNullOrEmpty()) {
                params["sessionuuid"] = sessionUuid
            }

            Log.d(TAG, "Retrieved connection params: $params")
            params
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connection params", e)
            mapOf("profileuuid" to config.profileUuid, "branch" to config.profileBranch)
        }
    }

    fun clearAll() {
        try {
            prefs.edit()
                .remove(MESSAGES_KEY)
                .remove(SESSION_UUID_KEY)
                .apply()
            Log.d(TAG, "Cleared all data from storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all data", e)
        }
    }
}
