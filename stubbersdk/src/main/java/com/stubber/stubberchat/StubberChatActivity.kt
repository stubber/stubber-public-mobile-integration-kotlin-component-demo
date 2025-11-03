package com.stubber.stubbersdk.stubberchat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.io.File
import java.io.IOException

class StubberChatActivity : AppCompatActivity() {

    private lateinit var config: ChatConfig
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var chatService: ChatService
    private lateinit var messageViewModel: MessageViewModel
    private lateinit var fileUploadService: FileUploadService
    private lateinit var filePreviewAdapter: FilePreviewAdapter
    private var selectedFiles: MutableList<Uri> = mutableListOf()
    private var filePreviewItems: MutableList<FilePreviewItem> = mutableListOf()

    // Voice recording
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var isRecording = false
    private var recordingStartTime = 0L
    private val recordingHandler = Handler(Looper.getMainLooper())
    private val recordingRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                val elapsed = (System.currentTimeMillis() - recordingStartTime) / 1000
                val minutes = elapsed / 60
                val seconds = elapsed % 60
                findViewById<TextView>(R.id.recordingTimer)?.text = String.format("%d:%02d", minutes, seconds)
                recordingHandler.postDelayed(this, 1000)
            }
        }
    }

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
        private const val RECORD_AUDIO_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Get configuration from intent
        config = intent.getParcelableExtra<ChatConfig>(StubberChatSDK.EXTRA_CHAT_CONFIG)
            ?: throw IllegalStateException("ChatConfig is required to start StubberChatActivity")

        val backgroundColor = config.backgroundColor
        val primaryColor = config.primaryColor
        val chatTitle = config.chatTitle

        // Handle keyboard insets
        val contentView = findViewById<android.view.View>(R.id.contentChat)

        // Set the background color
        contentView.setBackgroundColor(backgroundColor)

        ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Don't apply top padding - let AppBarLayout handle that via CoordinatorLayout behavior
            view.setPadding(
                systemBarsInsets.left,
                0,
                systemBarsInsets.right,
                imeInsets.bottom
            )
            insets
        }

        // Setup toolbar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        val appBarLayout = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)

        // Apply primary color to both toolbar and app bar
        toolbar.setBackgroundColor(primaryColor)
        appBarLayout?.setBackgroundColor(primaryColor)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = chatTitle

        // Initialize SDK service with config
        chatService = ChatService.getInstance(this, config)

        // Initialize file upload service with config
        fileUploadService = FileUploadService(this, config)

        // Initialize ViewModel
        messageViewModel = MessageViewModel(application, chatService)

        // Setup RecyclerView
        setupRecyclerView(primaryColor)

        // Setup file preview
        setupFilePreview()

        // Setup message input
        setupMessageInput(primaryColor)

        // Setup socket listeners
        setupSocketListeners()

        // Observe messages
        observeMessages()

        // Load messages from storage
        messageViewModel.loadMessages()

        // Connect to socket
        chatService.connect()
    }

    private fun setupRecyclerView(primaryColor: Int) {
        val recyclerView = findViewById<RecyclerView>(R.id.messagesRecyclerView)
        messageAdapter = MessageAdapter(
            R.layout.item_message_in,
            R.layout.item_message_out,
            primaryColor,
            config.fileServerUrl
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
        filePreviewAdapter = FilePreviewAdapter(
            onRemoveClick = { position ->
                // Remove file at position
                if (position < selectedFiles.size && position < filePreviewItems.size) {
                    selectedFiles.removeAt(position)
                    filePreviewItems.removeAt(position)
                    updateFilePreview()
                }
            },
            fileServerUrl = config.fileServerUrl
        )
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

        // Update send button icon based on file selection
        val messageEditText = findViewById<EditText>(R.id.messageEditText)
        val sendButton = findViewById<FloatingActionButton>(R.id.sendButton)
        if (messageEditText.text.isNullOrEmpty() && selectedFiles.isEmpty()) {
            sendButton.setImageResource(R.drawable.ic_mic)
        } else {
            sendButton.setImageResource(R.drawable.ic_send)
        }
    }

    private fun setupMessageInput(primaryColor: Int) {
        val messageEditText = findViewById<EditText>(R.id.messageEditText)
        val sendButton = findViewById<FloatingActionButton>(R.id.sendButton)
        val attachButton = findViewById<ImageButton>(R.id.attachButton)
        val cancelRecordingButton = findViewById<ImageButton>(R.id.cancelRecordingButton)

        // Apply primary color to send button
        sendButton.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)

        // Send/Record button - dynamic functionality based on text content
        sendButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                val messageText = messageEditText.text.toString().trim()
                if (messageText.isEmpty() && selectedFiles.isEmpty()) {
                    // No text, start voice recording
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
                    } else {
                        startRecording()
                    }
                } else {
                    // Has text or files, send message
                    sendMessage()
                }
            }
        }

        // Attachment button - always visible
        attachButton.setOnClickListener {
            openFilePicker()
        }

        // Cancel recording button
        cancelRecordingButton.setOnClickListener {
            cancelRecording()
        }

        // Update send button icon based on text input
        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty() && selectedFiles.isEmpty()) {
                    // Show mic icon when no text and no files
                    sendButton.setImageResource(R.drawable.ic_mic)
                } else {
                    // Show send icon when there's text or files
                    sendButton.setImageResource(R.drawable.ic_send)
                }
            }
        })
    }

    private fun startRecording() {
        try {
            // Create a temporary file for the recording
            val timestamp = System.currentTimeMillis()
            recordingFile = File(cacheDir, "voice_note_$timestamp.m4a")

            // Initialize MediaRecorder
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingFile!!.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            recordingStartTime = System.currentTimeMillis()

            // Show recording UI, hide text input and attach button, show cancel button
            val messageEditText = findViewById<EditText>(R.id.messageEditText)
            val recordingContainer = findViewById<android.view.View>(R.id.recordingContainer)
            val attachButton = findViewById<ImageButton>(R.id.attachButton)
            val cancelRecordingButton = findViewById<ImageButton>(R.id.cancelRecordingButton)
            val sendButton = findViewById<FloatingActionButton>(R.id.sendButton)

            messageEditText.visibility = android.view.View.GONE
            recordingContainer.visibility = android.view.View.VISIBLE
            attachButton.visibility = android.view.View.GONE
            cancelRecordingButton.visibility = android.view.View.VISIBLE

            // Change send button icon to send icon during recording
            sendButton.setImageResource(R.drawable.ic_send)

            // Start timer
            recordingHandler.post(recordingRunnable)

            Log.d(TAG, "Recording started: ${recordingFile!!.absolutePath}")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording", e)
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
            isRecording = false
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            // Stop timer
            recordingHandler.removeCallbacks(recordingRunnable)

            // Hide recording UI, show text input and attach button, hide cancel button
            val messageEditText = findViewById<EditText>(R.id.messageEditText)
            val recordingContainer = findViewById<android.view.View>(R.id.recordingContainer)
            val attachButton = findViewById<ImageButton>(R.id.attachButton)
            val cancelRecordingButton = findViewById<ImageButton>(R.id.cancelRecordingButton)
            val sendButton = findViewById<FloatingActionButton>(R.id.sendButton)

            messageEditText.visibility = android.view.View.VISIBLE
            recordingContainer.visibility = android.view.View.GONE
            attachButton.visibility = android.view.View.VISIBLE
            cancelRecordingButton.visibility = android.view.View.GONE

            // Restore mic icon on send button
            sendButton.setImageResource(R.drawable.ic_mic)

            // Upload and send the recording
            recordingFile?.let { file ->
                lifecycleScope.launch {
                    try {
                        // Give the file system a moment to flush the recording
                        kotlinx.coroutines.delay(100)

                        // Check if file has content
                        if (!file.exists() || file.length() == 0L) {
                            Log.e(TAG, "Recording file is empty or doesn't exist: ${file.absolutePath}")
                            Toast.makeText(this@StubberChatActivity, "Recording failed - empty file", Toast.LENGTH_SHORT).show()
                            file.delete()
                            recordingFile = null
                            return@launch
                        }

                        Log.d(TAG, "Recording file size: ${file.length()} bytes at ${file.absolutePath}")
                        Toast.makeText(this@StubberChatActivity, "Uploading voice note...", Toast.LENGTH_SHORT).show()

                        // Upload the file directly (not through URI conversion)
                        val uploadedAttachments = fileUploadService.uploadFilesDirectly(listOf(file))

                        if (uploadedAttachments.isNotEmpty()) {
                            // Send as a message with attachment
                            val message = Message(
                                direction = MessageDirection.OUTGOING,
                                message = "",
                                attachments = uploadedAttachments
                            )
                            messageViewModel.addMessage(message)

                            // Get connection params and send via socket
                            val connectionParams = chatService.getConnectionParams()
                            val outgoingPayload = OutgoingPayload.create(
                                connectionParams = connectionParams,
                                message = "",
                                attachments = uploadedAttachments
                            )
                            val payload = outgoingPayload.toJson()
                            chatService.emit("payload", payload)

                            Toast.makeText(this@StubberChatActivity, "Voice note sent", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@StubberChatActivity, "Failed to upload voice note", Toast.LENGTH_SHORT).show()
                        }

                        // Clean up
                        file.delete()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to upload voice note", e)
                        Toast.makeText(this@StubberChatActivity, "Failed to send voice note: ${e.message}", Toast.LENGTH_SHORT).show()
                        file.delete()
                    } finally {
                        recordingFile = null
                    }
                }
            }

            Log.d(TAG, "Recording stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            Toast.makeText(this, "Failed to stop recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelRecording() {
        try {
            // Stop and release MediaRecorder
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            // Stop timer
            recordingHandler.removeCallbacks(recordingRunnable)

            // Hide recording UI, show text input and attach button, hide cancel button
            val messageEditText = findViewById<EditText>(R.id.messageEditText)
            val recordingContainer = findViewById<android.view.View>(R.id.recordingContainer)
            val attachButton = findViewById<ImageButton>(R.id.attachButton)
            val cancelRecordingButton = findViewById<ImageButton>(R.id.cancelRecordingButton)
            val sendButton = findViewById<FloatingActionButton>(R.id.sendButton)

            messageEditText.visibility = android.view.View.VISIBLE
            recordingContainer.visibility = android.view.View.GONE
            attachButton.visibility = android.view.View.VISIBLE
            cancelRecordingButton.visibility = android.view.View.GONE

            // Restore mic icon on send button
            sendButton.setImageResource(R.drawable.ic_mic)

            // Delete the recording file
            recordingFile?.delete()
            recordingFile = null

            Toast.makeText(this, "Recording cancelled", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Recording cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel recording", e)
            Toast.makeText(this, "Failed to cancel recording", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                Toast.makeText(this, "Audio recording permission denied", Toast.LENGTH_SHORT).show()
            }
        }
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

        // Clean up recording resources
        if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaRecorder", e)
            }
            recordingHandler.removeCallbacks(recordingRunnable)
        }
        mediaRecorder = null
        recordingFile?.delete()

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