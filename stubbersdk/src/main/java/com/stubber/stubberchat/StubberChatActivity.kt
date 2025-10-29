package com.stubber.stubbersdk.stubberchat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stubber.stubbersdk.R
import com.stubber.stubbersdk.stubberchat.models.FilePreviewItem
import com.stubber.stubbersdk.stubberchat.models.Message
import com.stubber.stubbersdk.stubberchat.models.MessageDirection
import com.stubber.stubbersdk.stubberchat.models.OutgoingPayload
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class StubberChatActivity : AppCompatActivity() {

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var chatService: ChatService
    private lateinit var messageViewModel: MessageViewModel
    private lateinit var fileUploadService: FileUploadService
    private lateinit var filePreviewAdapter: FilePreviewAdapter
    private var selectedFiles: MutableList<Uri> = mutableListOf()
    private var filePreviewItems: MutableList<FilePreviewItem> = mutableListOf()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val uris = mutableListOf<Uri>()

            // Handle multiple files
            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i).uri?.let { uri ->
                        uris.add(uri)
                    }
                }
            } ?: data?.data?.let { uri ->
                // Handle single file
                uris.add(uri)
            }

            if (uris.isNotEmpty()) {
                selectedFiles.addAll(uris)
                filePreviewItems.addAll(uris.map { FilePreviewItem(it) })
                updateFilePreview()
                Toast.makeText(this, "${uris.size} file(s) selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val TAG = "StubberChatActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Handle keyboard insets
        val contentView = findViewById<android.view.View>(R.id.contentChat)
        ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                imeInsets.bottom
            )
            insets
        }

        // Setup toolbar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chat"

        // Get configuration from SDK
        val config = StubberChatSDK.getConfig()

        // Initialize SDK service
        chatService = ChatService.getInstance(this, config)

        // Initialize file upload service
        fileUploadService = FileUploadService(this, config)

        // Initialize ViewModel
        messageViewModel = MessageViewModel(application, chatService)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup file preview
        setupFilePreview()

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
            layoutManager = LinearLayoutManager(this@StubberChatActivity).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupFilePreview() {
        val previewRecyclerView = findViewById<RecyclerView>(R.id.selectedFilesPreview)
        filePreviewAdapter = FilePreviewAdapter { position ->
            // Remove file at position
            if (position < selectedFiles.size && position < filePreviewItems.size) {
                selectedFiles.removeAt(position)
                filePreviewItems.removeAt(position)
                updateFilePreview()
            }
        }
        previewRecyclerView.apply {
            adapter = filePreviewAdapter
            layoutManager = LinearLayoutManager(
                this@StubberChatActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }
    }

    private fun updateFilePreview() {
        val previewRecyclerView = findViewById<RecyclerView>(R.id.selectedFilesPreview)
        filePreviewAdapter.submitList(filePreviewItems.toList())
        previewRecyclerView.visibility = if (filePreviewItems.isNotEmpty()) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
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
            openFilePicker()
        }

        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Could update send button state here
            }
        })
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(Intent.createChooser(intent, "Select files"))
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

        if (messageText.isEmpty() && selectedFiles.isEmpty()) {
            return
        }

        lifecycleScope.launch {
            try {
                // Upload files if any are selected
                val attachments = if (selectedFiles.isNotEmpty()) {
                    Toast.makeText(this@StubberChatActivity, "Uploading files...", Toast.LENGTH_SHORT).show()
                    val uploadedAttachments = fileUploadService.uploadFiles(selectedFiles)

                    // Update preview items with uploaded attachments
                    filePreviewItems.clear()
                    selectedFiles.forEachIndexed { index, uri ->
                        if (index < uploadedAttachments.size) {
                            filePreviewItems.add(FilePreviewItem(uri, uploadedAttachments[index]))
                        }
                    }
                    updateFilePreview()

                    uploadedAttachments
                } else {
                    emptyList()
                }

                // Add message to UI
                val message = Message(
                    direction = MessageDirection.OUTGOING,
                    message = messageText,
                    attachments = attachments
                )
                messageViewModel.addMessage(message)

                // Get connection params from storage
                val connectionParams = chatService.getConnectionParams()

                // Build the payload using OutgoingPayload model
                val outgoingPayload = OutgoingPayload.create(
                    connectionParams = connectionParams,
                    message = messageText,
                    attachments = attachments
                )
                val payload = outgoingPayload.toJson()

                Log.d(TAG, "Sending payload: $payload")

                // Emit the message via socket
                chatService.emit("payload", payload)

                // Clear input and preview
                messageEditText.text.clear()
                selectedFiles.clear()
                filePreviewItems.clear()
                updateFilePreview()

                if (attachments.isNotEmpty()) {
                    Toast.makeText(this@StubberChatActivity, "Message sent", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                Toast.makeText(this@StubberChatActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
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