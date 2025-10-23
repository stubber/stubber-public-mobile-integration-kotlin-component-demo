package com.stubber.stubberchatsdk.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stubber.stubberchatsdk.R
import org.json.JSONArray
import org.json.JSONObject

class ChatActivity : AppCompatActivity() {

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var chatService: ChatService
    private lateinit var messageViewModel: MessageViewModel

    companion object {
        private const val TAG = "ChatActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Setup toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chat"

        // Get configuration from SDK
        val config = StubberChatSDK.getConfig()

        // Initialize SDK service
        chatService = ChatService.getInstance(this, config)

        // Initialize ViewModel
        messageViewModel = MessageViewModel(application, chatService)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup message input
        setupMessageInput()

        // Setup socket listeners
        setupSocketListeners()

        // Observe messages
        observeMessages()

        // Load messages from storage
        messageViewModel.loadMessages()

        // Connect to socket
        chatService.connect()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.messagesRecyclerView)
        messageAdapter = MessageAdapter(
            R.layout.item_message_in,
            R.layout.item_message_out
        )
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
        Log.d(TAG, "Clearing session UUID and messages from storage")

        messageViewModel.clearMessages()
        chatService.clearSessionUuid()

        finish()
    }

    private fun reloadConnection() {
        Log.d(TAG, "Reloading connection - clearing session UUID and messages")

        messageViewModel.clearMessages()
        chatService.clearSessionUuid()

        chatService.disconnect()
        chatService.connect()
    }
}