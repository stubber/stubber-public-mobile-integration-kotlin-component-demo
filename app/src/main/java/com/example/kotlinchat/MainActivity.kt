package com.example.kotlinchat

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinchat.adapter.MessageAdapter
import com.example.kotlinchat.config.Environment
import com.example.kotlinchat.databinding.ActivityMainBinding
import com.example.kotlinchat.models.Attachment
import com.example.kotlinchat.models.UploadingAttachment
import com.example.kotlinchat.services.FileUploadService
import com.example.kotlinchat.services.SocketService
import com.example.kotlinchat.services.StorageService
import com.example.kotlinchat.viewmodel.MessageViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var socketService: SocketService
    private lateinit var storageService: StorageService
    private lateinit var fileUploadService: FileUploadService
    private lateinit var drawerLayout: DrawerLayout

    private val messageViewModel: MessageViewModel by viewModels()

    private val selectedFileUris = mutableListOf<Uri>()
    private val completedAttachments = mutableListOf<Map<String, Any>>()
    private var isUploading = false

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            handleFileSelection(uris)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Print environment info
        Environment.printEnvironmentInfo()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Initialize services
        socketService = SocketService.getInstance(this)
        storageService = StorageService(this)
        fileUploadService = FileUploadService(this)

        // Setup drawer
        drawerLayout = binding.drawerLayout
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.toolbar,
            R.string.app_name, R.string.app_name
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Setup RecyclerView
        setupRecyclerView()

        // Setup message input
        setupMessageInput()

        // Setup settings drawer
        setupSettingsDrawer()

        // Setup socket listeners
        setupSocketListeners()

        // Observe messages
        observeMessages()

        // Connect to socket
        socketService.connect()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.messagesRecyclerView)
        messageAdapter = MessageAdapter()
        recyclerView.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                reverseLayout = true
                stackFromEnd = true
            }
        }
    }

    private fun setupMessageInput() {
        val messageEditText = findViewById<EditText>(R.id.messageEditText)
        val sendButton = findViewById<FloatingActionButton>(R.id.sendButton)
        val attachButton = findViewById<ImageButton>(R.id.attachButton)

        sendButton.setOnClickListener {
            sendMessage()
        }

        attachButton.setOnClickListener {
            if (!isUploading) {
                filePickerLauncher.launch("*/*")
            }
        }

        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Could update send button state here
            }
        })
    }

    private fun setupSettingsDrawer() {
        val profileCodeEditText = findViewById<EditText>(R.id.profileCodeEditText)
        val resetSessionButton = findViewById<Button>(R.id.resetSessionButton)
        val connectionStatusText = findViewById<TextView>(R.id.connectionStatusText)

        // Load saved profile code
        val savedProfileCode = storageService.loadProfileCode()
        if (savedProfileCode != null) {
            profileCodeEditText.setText(savedProfileCode)
        }

        // Save profile code on change
        profileCodeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                storageService.saveProfileCode(s?.toString())
            }
        })

        // Reset session button
        resetSessionButton.setOnClickListener {
            messageViewModel.clearMessages()
            storageService.clearSessionUuid()
            socketService.disconnect()
            socketService.connect()
            Toast.makeText(this, "Session reset - new session will be assigned by server", Toast.LENGTH_SHORT).show()
        }

        // Update connection status
        updateConnectionStatus(connectionStatusText)
    }

    private fun setupSocketListeners() {
        socketService.setMessageCallback { payload ->
            runOnUiThread {
                messageViewModel.addIncomingMessage(payload)
            }
        }

        socketService.setSessionUuidCallback { sessionUuid ->
            Log.d(TAG, "Session UUID received: $sessionUuid")
        }
    }

    private fun observeMessages() {
        messageViewModel.messages.observe(this) { messages ->
            messageAdapter.submitList(messages)
            findViewById<RecyclerView>(R.id.messagesRecyclerView).scrollToPosition(0)
        }
    }

    private fun handleFileSelection(uris: List<Uri>) {
        isUploading = true
        lifecycleScope.launch {
            try {
                val serverUrl = Environment.serverUrl
                val profileCodesessionUuid = storageService.loadProfileCode() ?: "ROTOBP"

                val uploadResults = fileUploadService.uploadMultipleFiles(uris, serverUrl, "ROTOBP")

                if (uploadResults.isNotEmpty()) {
                    completedAttachments.addAll(uploadResults)
                    Toast.makeText(this@MainActivity, "Uploaded ${uploadResults.size} files", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to upload files", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading files", e)
                Toast.makeText(this@MainActivity, "Error uploading files", Toast.LENGTH_SHORT).show()
            } finally {
                isUploading = false
            }
        }
    }

    private fun sendMessage() {
        val messageEditText = findViewById<EditText>(R.id.messageEditText)
        val messageText = messageEditText.text.toString().trim()

        val hasText = messageText.isNotEmpty()
        val hasCompletedAttachments = completedAttachments.isNotEmpty()

        if (hasText || hasCompletedAttachments) {
            // Convert completed attachment objects to Attachment models
            val attachments = completedAttachments.map { attachment ->
                Attachment(
                    filename = attachment["filename"] as String,
                    originalname = attachment["originalname"] as String,
                    fileuuid = attachment["fileuuid"] as String,
                    contentType = attachment["contentType"] as String
                )
            }

            messageViewModel.addOutgoingMessageWithAttachments(
                messageText,
                attachments = attachments
            )

            // Get connection params from storage
            val connectionParams = storageService.getConnectionParams()

            // Convert attachments list to JSONArray
            val attachmentsArray = JSONArray()
            // completedAttachments.forEach { attachment ->
            //     attachmentsArray.put(JSONObject(attachment))
            // }

            // Build the payload
            val payload = JSONObject().apply {
                connectionParams.forEach { (key, value) ->
                    put(key, value)
                }
                put("message", messageText)
                put("data", messageText)
                put("type", "text")
                put("attachments", attachmentsArray)
            }

            Log.d(TAG, "Sending payload: $payload")

            // Emit the message via socket
            socketService.emit("payload", payload)

            // Clear input and attachments
            messageEditText.text.clear()
            completedAttachments.clear()
        }
    }

    private fun updateConnectionStatus(statusText: TextView) {
        if (socketService.isConnected()) {
            statusText.text = "Connected"
            statusText.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            statusText.text = "Disconnected"
            statusText.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socketService.disconnect()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}