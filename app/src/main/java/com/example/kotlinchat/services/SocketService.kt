package com.example.kotlinchat.services

import android.content.Context
import android.util.Log
import com.example.kotlinchat.config.Environment
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class SocketService private constructor(private val context: Context) {
    private var socket: Socket? = null
    private var isConnected = false
    private var messageCallback: ((JSONObject) -> Unit)? = null
    private var sessionUuidCallback: ((String) -> Unit)? = null
    private val storageService = StorageService(context)

    companion object {
        private const val TAG = "SocketService"

        @Volatile
        private var instance: SocketService? = null

        fun getInstance(context: Context): SocketService {
            return instance ?: synchronized(this) {
                instance ?: SocketService(context.applicationContext).also { instance = it }
            }
        }
    }

    // Getters
    fun isConnected(): Boolean = isConnected

    fun getSocket(): Socket? = socket

    // Set message callback for handling incoming messages
    fun setMessageCallback(callback: (JSONObject) -> Unit) {
        messageCallback = callback
    }

    // Set session UUID callback
    fun setSessionUuidCallback(callback: (String) -> Unit) {
        sessionUuidCallback = callback
    }

    // Initialize and connect to socket
    fun connect() {
        try {
            val serverUrl = Environment.serverUrl
            val opts = IO.Options().apply {
                path = "/v2/socket.io/"
                transports = arrayOf("websocket", "polling")
                reconnection = true
            }

            val newSocket = IO.socket(serverUrl, opts)
            setupListeners(newSocket)
            newSocket.connect()
            socket = newSocket
            Log.d(TAG, "Socket connecting to: $serverUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing socket", e)
        }
    }

    // Disconnect socket
    fun disconnect() {
        socket?.let {
            it.disconnect()
            it.off()
            socket = null
            isConnected = false
            Log.d(TAG, "Socket disconnected")
        }
    }

    // Send message/event
    fun emit(event: String, data: Any) {
        if (socket != null && isConnected) {
            socket?.emit(event, data)
            Log.d(TAG, "Emitted event: $event with data: $data")
        } else {
            Log.w(TAG, "Socket not connected. Cannot emit event: $event")
        }
    }

    // Setup event listeners
    private fun setupListeners(socket: Socket) {
        // Connection event
        socket.on(Socket.EVENT_CONNECT) {
            isConnected = true
            Log.d(TAG, "Socket connected")

            // Emit config with connection params
            val query = storageService.getConnectionParams()
            val configData = JSONObject(query as Map<*, *>)
            socket.emit("config", configData)
            Log.d(TAG, ">>> Using connection params from storage: $query")
        }

        // Disconnection event
        socket.on(Socket.EVENT_DISCONNECT) {
            isConnected = false
            Log.d(TAG, "Socket disconnected")
        }

        // Connection error
        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e(TAG, "Connection error: ${args.joinToString()}")
        }

        // Listen for session UUID from server
        socket.on("sessionuuid") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as? JSONObject
                val sessionUuid = data?.optString("session")
                if (!sessionUuid.isNullOrEmpty()) {
                    storageService.saveSessionUuid(sessionUuid)
                    sessionUuidCallback?.invoke(sessionUuid)
                    Log.d(TAG, "Session UUID updated to: $sessionUuid")
                }
            }
        }

        // Listen for payload events from server
        socket.on("payload") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as? JSONObject
                if (data != null) {
                    Log.d(TAG, "Payload received: $data")

                    // Handle incoming payload with expected format: {data, type, attachments}
                    if (data.has("data") && data.optString("type") == "text") {
                        Log.d(TAG, "Incoming message: ${data.optString("data")}")
                        messageCallback?.invoke(data)
                    }
                }
            }
        }
    }
}
