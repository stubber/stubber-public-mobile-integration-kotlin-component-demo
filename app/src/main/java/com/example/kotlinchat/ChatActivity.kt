package com.example.kotlinchat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinchat.adapter.MessageAdapter
import com.example.kotlinchat.config.Environment
import com.example.kotlinchat.databinding.ActivityChatBinding
import com.example.kotlinchat.services.ChatService
import com.example.kotlinchat.viewmodel.MessageViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var chatService: ChatService

    private val messageViewModel: MessageViewModel by viewModels()

    companion object {
        private const val TAG = "ChatActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Print environment info
        Environment.printEnvironmentInfo()

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize service
        chatService = ChatService.getInstance(this)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup message input
        setupMessageInput()

        // Setup socket listeners
        setupSocketListeners()

        // Observe messages
        observeMessages()

        // Connect to socket
        chatService.connect()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.messagesRecyclerView)
        messageAdapter = MessageAdapter()
        recyclerView.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupMessageInput() {
        val messageEditText = findViewById<EditText>(R.id.messageEditText)
        val sendButton = findViewById<FloatingActionButton>(R.id.sendButton)

        sendButton.setOnClickListener {
            sendMessage()
        }

        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Could update send button state here
            }
        })
    }

    private fun setupSocketListeners() {
        chatService.setMessageCallback { payload ->
            runOnUiThread {
                messageViewModel.addIncomingMessage(payload)
            }
        }

        chatService.setSessionUuidCallback { sessionUuid ->
            Log.d(TAG, "Session UUID received: $sessionUuid")
        }
    }

    private fun observeMessages() {
        messageViewModel.messages.observe(this) { messages ->
            messageAdapter.submitList(messages)
            if (messages.isNotEmpty()) {
                findViewById<RecyclerView>(R.id.messagesRecyclerView).scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun sendMessage() {
        val messageEditText = findViewById<EditText>(R.id.messageEditText)
        val messageText = messageEditText.text.toString().trim()

        if (messageText.isNotEmpty()) {
            messageViewModel.addOutgoingMessage(messageText)

            // Get connection params from storage
            val connectionParams = chatService.getConnectionParams()

            // Build the payload
            val payload = JSONObject().apply {
                connectionParams.forEach { (key, value) ->
                    put(key, value)
                }
                put("message", messageText)
                put("data", messageText)
                put("type", "text")
                put("attachments", JSONArray())
            }

            Log.d(TAG, "Sending payload: $payload")

            // Emit the message via socket
            chatService.emit("payload", payload)

            // Clear input
            messageEditText.text.clear()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatService.disconnect()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_session -> {
                clearSession()
                true
            }
            R.id.action_reload -> {
                reloadConnection()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun clearSession() {
        Log.d(TAG, "Clearing session UUID from storage")
        chatService.clearSessionUuid()
    }

    private fun reloadConnection() {
        Log.d(TAG, "Reloading connection - clearing session UUID and messages")

        // Clear messages
        messageViewModel.clearMessages()

        // Clear session UUID from storage
        chatService.clearSessionUuid()

        // Disconnect current connection
        chatService.disconnect()

        // Reconnect with new session
        chatService.connect()
    }
}