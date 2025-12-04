package com.stubber.mobilechat

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.stubber.mobilechat.databinding.ActivityMainBinding
import com.stubber.stubbersdk.stubberchat.ChatConfig
import com.stubber.stubbersdk.stubberchat.StubberChatSDK

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bgColorPreview: View
    private lateinit var actionBarColorPreview: View
    private lateinit var sendButtonColorPreview: View
    private lateinit var profileConfigManager: ProfileConfigurationManager

    // Server configurations
    companion object {
        // Debug build config
        // private const val DEBUG_SERVER_URL = "http://192.168.254.32:6020"
        // private const val DEBUG_FILE_SERVER_URL = "https://app.dev.stubber.com/api/fileserver/file"
        private const val DEBUG_SERVER_URL = "https://api.stubber.zone:6020"
        private const val DEBUG_FILE_SERVER_URL = "https://app.stubber.com/api/fileserver/file"

        // Release build config
        private const val RELEASE_SERVER_URL = "https://api.stubber.zone:6020"
        private const val RELEASE_FILE_SERVER_URL = "https://app.stubber.com/api/fileserver/file"

        // SharedPreferences keys
        private const val PREFS_NAME = "stubber_demo_prefs"
        private const val KEY_PROFILE_CODE = "profile_code"
        private const val KEY_CHAT_TITLE = "chat_title"
        private const val KEY_BG_COLOR = "bg_color"
        private const val KEY_ACTION_BAR_COLOR = "action_bar_color"
        private const val KEY_SEND_BUTTON_COLOR = "send_button_color"
        private const val KEY_IS_DRAFT = "is_draft"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Stubber Chat"

        val profileCodeInput = findViewById<TextInputEditText>(R.id.profileCodeInput)
        val chatTitleInput = findViewById<TextInputEditText>(R.id.chatTitleInput)
        val bgColorInput = findViewById<TextInputEditText>(R.id.bgColorInput)
        val actionBarColorInput = findViewById<TextInputEditText>(R.id.actionBarColorInput)
        val sendButtonColorInput = findViewById<TextInputEditText>(R.id.sendButtonColorInput)
        val outgoingBubbleColorInput = findViewById<TextInputEditText>(R.id.outgoingBubbleColorInput)
        val outgoingTextColorInput = findViewById<TextInputEditText>(R.id.outgoingTextColorInput)
        val incomingBubbleColorInput = findViewById<TextInputEditText>(R.id.incomingBubbleColorInput)
        val incomingTextColorInput = findViewById<TextInputEditText>(R.id.incomingTextColorInput)
        val actionBarTextColorInput = findViewById<TextInputEditText>(R.id.actionBarTextColorInput)
        val openChatButton = findViewById<Button>(R.id.openChatButton)

        bgColorPreview = findViewById(R.id.bgColorPreview)
        actionBarColorPreview = findViewById(R.id.actionBarColorPreview)
        sendButtonColorPreview = findViewById(R.id.sendButtonColorPreview)
        val outgoingBubbleColorPreview = findViewById<View>(R.id.outgoingBubbleColorPreview)
        val outgoingTextColorPreview = findViewById<View>(R.id.outgoingTextColorPreview)
        val incomingBubbleColorPreview = findViewById<View>(R.id.incomingBubbleColorPreview)
        val incomingTextColorPreview = findViewById<View>(R.id.incomingTextColorPreview)
        val actionBarTextColorPreview = findViewById<View>(R.id.actionBarTextColorPreview)

        // Initialize profile configuration manager
        profileConfigManager = ProfileConfigurationManager(this)

        // Setup profile management buttons
        val saveProfileButton = findViewById<Button>(R.id.saveProfileButton)
        val loadProfileButton = findViewById<Button>(R.id.loadProfileButton)

        saveProfileButton.setOnClickListener {
            showSaveProfileDialog()
        }

        loadProfileButton.setOnClickListener {
            showLoadProfileDialog()
        }

        // Load saved values
        loadSavedValues()

        // Update background color preview as user types
        bgColorInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val colorString = s.toString().trim()
                if (colorString.isNotEmpty()) {
                    updateColorPreview(bgColorPreview, colorString)
                } else {
                    updateColorPreview(bgColorPreview, "#ECE5DD")
                }
            }
        })

        // Update action bar color preview as user types
        actionBarColorInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val colorString = s.toString().trim()
                if (colorString.isNotEmpty()) {
                    updateColorPreview(actionBarColorPreview, colorString)
                } else {
                    updateColorPreview(actionBarColorPreview, "#0845a6")
                }
            }
        })

        // Update send button color preview as user types
        sendButtonColorInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val colorString = s.toString().trim()
                if (colorString.isNotEmpty()) {
                    updateColorPreview(sendButtonColorPreview, colorString)
                } else {
                    updateColorPreview(sendButtonColorPreview, "#DCF415")
                }
            }
        })

        // Update outgoing bubble color preview as user types
        outgoingBubbleColorInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val colorString = s.toString().trim()
                if (colorString.isNotEmpty()) {
                    updateColorPreview(outgoingBubbleColorPreview, colorString)
                } else {
                    updateColorPreview(outgoingBubbleColorPreview, "#DCF415")
                }
            }
        })

        // Update outgoing text color preview as user types
        outgoingTextColorInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val colorString = s.toString().trim()
                if (colorString.isNotEmpty()) {
                    updateColorPreview(outgoingTextColorPreview, colorString)
                } else {
                    updateColorPreview(outgoingTextColorPreview, "#000000")
                }
            }
        })

        // Update incoming bubble color preview as user types
        incomingBubbleColorInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val colorString = s.toString().trim()
                if (colorString.isNotEmpty()) {
                    updateColorPreview(incomingBubbleColorPreview, colorString)
                } else {
                    updateColorPreview(incomingBubbleColorPreview, "#FFFFFF")
                }
            }
        })

        // Update incoming text color preview as user types
        incomingTextColorInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val colorString = s.toString().trim()
                if (colorString.isNotEmpty()) {
                    updateColorPreview(incomingTextColorPreview, colorString)
                } else {
                    updateColorPreview(incomingTextColorPreview, "#000000")
                }
            }
        })

        // Update action bar text color preview as user types
        actionBarTextColorInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val colorString = s.toString().trim()
                if (colorString.isNotEmpty()) {
                    updateColorPreview(actionBarTextColorPreview, colorString)
                } else {
                    updateColorPreview(actionBarTextColorPreview, "#FFFFFF")
                }
            }
        })

        val radioDraft = findViewById<RadioButton>(R.id.radioDraft)

        openChatButton.setOnClickListener {
            val profileCode = profileCodeInput.text.toString().trim()
            val chatTitle = chatTitleInput.text.toString().trim()
            val bgColorString = bgColorInput.text.toString().trim()
            val actionBarColorString = actionBarColorInput.text.toString().trim()
            val sendButtonColorString = sendButtonColorInput.text.toString().trim()
            val outgoingBubbleColorString = outgoingBubbleColorInput.text.toString().trim()
            val outgoingTextColorString = outgoingTextColorInput.text.toString().trim()
            val incomingBubbleColorString = incomingBubbleColorInput.text.toString().trim()
            val incomingTextColorString = incomingTextColorInput.text.toString().trim()
            val actionBarTextColorString = actionBarTextColorInput.text.toString().trim()
            val isDraft = radioDraft.isChecked

            // Validate profile code
            if (profileCode.isEmpty()) {
                Toast.makeText(this, "Please enter a profile code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Use custom title or default to "Sanlam"
            val title = if (chatTitle.isEmpty()) "Sanlam" else chatTitle

            // Parse background color or use default if empty
            val backgroundColor = if (bgColorString.isEmpty()) {
                Color.parseColor("#ECE5DD")
            } else {
                try {
                    Color.parseColor(bgColorString)
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(this, "Invalid background color format. Use hex color (e.g. #ECE5DD)", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Parse action bar color or use default if empty
            val actionBarColor = if (actionBarColorString.isEmpty()) {
                Color.parseColor("#0845a6")
            } else {
                try {
                    Color.parseColor(actionBarColorString)
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(this, "Invalid action bar color format", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Parse send button color or use default if empty
            val sendButtonColor = if (sendButtonColorString.isEmpty()) {
                Color.parseColor("#DCF415")
            } else {
                try {
                    Color.parseColor(sendButtonColorString)
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(this, "Invalid send button color format", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Parse outgoing bubble color or use default if empty
            val outgoingBubbleColor = if (outgoingBubbleColorString.isEmpty()) {
                Color.parseColor("#DCF415")
            } else {
                try {
                    Color.parseColor(outgoingBubbleColorString)
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(this, "Invalid outgoing bubble color format", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Parse outgoing text color or use default if empty
            val outgoingTextColor = if (outgoingTextColorString.isEmpty()) {
                Color.parseColor("#000000")
            } else {
                try {
                    Color.parseColor(outgoingTextColorString)
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(this, "Invalid outgoing text color format", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Parse incoming bubble color or use default if empty
            val incomingBubbleColor = if (incomingBubbleColorString.isEmpty()) {
                Color.parseColor("#FFFFFF")
            } else {
                try {
                    Color.parseColor(incomingBubbleColorString)
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(this, "Invalid incoming bubble color format", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Parse incoming text color or use default if empty
            val incomingTextColor = if (incomingTextColorString.isEmpty()) {
                Color.parseColor("#000000")
            } else {
                try {
                    Color.parseColor(incomingTextColorString)
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(this, "Invalid incoming text color format", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Parse action bar text color or use default if empty
            val actionBarTextColor = if (actionBarTextColorString.isEmpty()) {
                Color.parseColor("#FFFFFF")
            } else {
                try {
                    Color.parseColor(actionBarTextColorString)
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(this, "Invalid action bar text color format", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Select server configuration based on build type (debug vs release)
            val serverUrl = if (BuildConfig.DEBUG) DEBUG_SERVER_URL else RELEASE_SERVER_URL
            val fileServerUrl = if (BuildConfig.DEBUG) DEBUG_FILE_SERVER_URL else RELEASE_FILE_SERVER_URL

            // Draft/Live only affects the branch parameter
            val profileBranch = if (isDraft) "draft" else "live"

            // Create ChatConfig
            val config = ChatConfig(
                serverUrl = serverUrl,
                fileServerUrl = fileServerUrl,
                profileCode = profileCode,
                profileBranch = profileBranch,
                backgroundColor = backgroundColor,
                actionBarColor = actionBarColor,
                sendButtonColor = sendButtonColor,
                chatTitle = title,
                outgoingBubbleColor = outgoingBubbleColor,
                outgoingTextColor = outgoingTextColor,
                incomingBubbleColor = incomingBubbleColor,
                incomingTextColor = incomingTextColor,
                actionBarTextColor = actionBarTextColor
            )

            // Launch chat using SDK with the configuration
            StubberChatSDK.startChat(this, config)
        }
    }

    private fun updateColorPreview(preview: View, colorString: String) {
        try {
            val color = Color.parseColor(colorString)
            preview.setBackgroundColor(color)
        } catch (e: IllegalArgumentException) {
            // Invalid color, keep previous preview
        }
    }

    private fun loadSavedValues() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val profileCodeInput = findViewById<TextInputEditText>(R.id.profileCodeInput)
        val chatTitleInput = findViewById<TextInputEditText>(R.id.chatTitleInput)
        val bgColorInput = findViewById<TextInputEditText>(R.id.bgColorInput)
        val actionBarColorInput = findViewById<TextInputEditText>(R.id.actionBarColorInput)
        val sendButtonColorInput = findViewById<TextInputEditText>(R.id.sendButtonColorInput)
        val radioDraft = findViewById<RadioButton>(R.id.radioDraft)
        val radioLive = findViewById<RadioButton>(R.id.radioLive)

        // Load saved values
        profileCodeInput.setText(prefs.getString(KEY_PROFILE_CODE, ""))
        chatTitleInput.setText(prefs.getString(KEY_CHAT_TITLE, ""))
        bgColorInput.setText(prefs.getString(KEY_BG_COLOR, ""))
        actionBarColorInput.setText(prefs.getString(KEY_ACTION_BAR_COLOR, ""))
        sendButtonColorInput.setText(prefs.getString(KEY_SEND_BUTTON_COLOR, ""))

        val isDraft = prefs.getBoolean(KEY_IS_DRAFT, true)
        if (isDraft) {
            radioDraft.isChecked = true
        } else {
            radioLive.isChecked = true
        }

        // Update color previews
        val bgColor = prefs.getString(KEY_BG_COLOR, "")
        val actionBarColor = prefs.getString(KEY_ACTION_BAR_COLOR, "")
        val sendButtonColor = prefs.getString(KEY_SEND_BUTTON_COLOR, "")

        if (bgColor.isNullOrEmpty()) {
            updateColorPreview(bgColorPreview, "#ECE5DD")
        } else {
            updateColorPreview(bgColorPreview, bgColor)
        }

        if (actionBarColor.isNullOrEmpty()) {
            updateColorPreview(actionBarColorPreview, "#0845a6")
        } else {
            updateColorPreview(actionBarColorPreview, actionBarColor)
        }

        if (sendButtonColor.isNullOrEmpty()) {
            updateColorPreview(sendButtonColorPreview, "#DCF415")
        } else {
            updateColorPreview(sendButtonColorPreview, sendButtonColor)
        }
    }

    private fun saveValues() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()

        val profileCodeInput = findViewById<TextInputEditText>(R.id.profileCodeInput)
        val chatTitleInput = findViewById<TextInputEditText>(R.id.chatTitleInput)
        val bgColorInput = findViewById<TextInputEditText>(R.id.bgColorInput)
        val actionBarColorInput = findViewById<TextInputEditText>(R.id.actionBarColorInput)
        val sendButtonColorInput = findViewById<TextInputEditText>(R.id.sendButtonColorInput)
        val radioDraft = findViewById<RadioButton>(R.id.radioDraft)

        editor.putString(KEY_PROFILE_CODE, profileCodeInput.text.toString())
        editor.putString(KEY_CHAT_TITLE, chatTitleInput.text.toString())
        editor.putString(KEY_BG_COLOR, bgColorInput.text.toString())
        editor.putString(KEY_ACTION_BAR_COLOR, actionBarColorInput.text.toString())
        editor.putString(KEY_SEND_BUTTON_COLOR, sendButtonColorInput.text.toString())
        editor.putBoolean(KEY_IS_DRAFT, radioDraft.isChecked)

        editor.apply()
    }

    override fun onPause() {
        super.onPause()
        saveValues()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private fun showSaveProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_save_profile, null)
        val profileNameInput = dialogView.findViewById<EditText>(R.id.dialogProfileNameInput)

        AlertDialog.Builder(this)
            .setTitle(R.string.save_profile)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val profileName = profileNameInput.text.toString().trim()
                if (profileName.isEmpty()) {
                    Toast.makeText(this, R.string.profile_name_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (profileConfigManager.profileExists(profileName)) {
                    // Show confirmation dialog for overwrite
                    AlertDialog.Builder(this)
                        .setTitle("Overwrite Profile?")
                        .setMessage("A profile with this name already exists. Do you want to overwrite it?")
                        .setPositiveButton("Overwrite") { _, _ ->
                            saveCurrentProfile(profileName)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    saveCurrentProfile(profileName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveCurrentProfile(profileName: String) {
        val profileCodeInput = findViewById<TextInputEditText>(R.id.profileCodeInput)
        val chatTitleInput = findViewById<TextInputEditText>(R.id.chatTitleInput)
        val bgColorInput = findViewById<TextInputEditText>(R.id.bgColorInput)
        val actionBarColorInput = findViewById<TextInputEditText>(R.id.actionBarColorInput)
        val sendButtonColorInput = findViewById<TextInputEditText>(R.id.sendButtonColorInput)
        val outgoingBubbleColorInput = findViewById<TextInputEditText>(R.id.outgoingBubbleColorInput)
        val outgoingTextColorInput = findViewById<TextInputEditText>(R.id.outgoingTextColorInput)
        val incomingBubbleColorInput = findViewById<TextInputEditText>(R.id.incomingBubbleColorInput)
        val incomingTextColorInput = findViewById<TextInputEditText>(R.id.incomingTextColorInput)
        val actionBarTextColorInput = findViewById<TextInputEditText>(R.id.actionBarTextColorInput)
        val radioDraft = findViewById<RadioButton>(R.id.radioDraft)

        val profile = ProfileConfiguration(
            name = profileName,
            profileCode = profileCodeInput.text.toString().trim(),
            isDraft = radioDraft.isChecked,
            chatTitle = chatTitleInput.text.toString().trim(),
            bgColor = bgColorInput.text.toString().trim(),
            actionBarColor = actionBarColorInput.text.toString().trim().ifEmpty { "#0845a6" },
            sendButtonColor = sendButtonColorInput.text.toString().trim().ifEmpty { "#DCF415" },
            outgoingBubbleColor = outgoingBubbleColorInput.text.toString().trim().ifEmpty { "#DCF415" },
            outgoingTextColor = outgoingTextColorInput.text.toString().trim().ifEmpty { "#000000" },
            incomingBubbleColor = incomingBubbleColorInput.text.toString().trim().ifEmpty { "#FFFFFF" },
            incomingTextColor = incomingTextColorInput.text.toString().trim().ifEmpty { "#000000" },
            actionBarTextColor = actionBarTextColorInput.text.toString().trim().ifEmpty { "#FFFFFF" }
        )

        if (profileConfigManager.saveProfile(profile)) {
            Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoadProfileDialog() {
        val profileNames = profileConfigManager.getProfileNames()

        if (profileNames.isEmpty()) {
            Toast.makeText(this, R.string.no_saved_profiles, Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_load_profile, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.profilesRecyclerView)

        val profiles = profileNames.mapNotNull { profileConfigManager.loadProfile(it) }

        val adapter = SavedProfilesAdapter(
            onProfileClick = { profile ->
                loadProfile(profile)
            },
            onDeleteClick = { profile ->
                AlertDialog.Builder(this)
                    .setTitle(R.string.delete_profile)
                    .setMessage(R.string.confirm_delete_profile)
                    .setPositiveButton("Delete") { dialog, _ ->
                        if (profileConfigManager.deleteProfile(profile.name)) {
                            Toast.makeText(this, R.string.profile_deleted, Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            // Refresh the dialog
                            showLoadProfileDialog()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        adapter.submitList(profiles)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.load_profile)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        // Update the adapter's click handler to dismiss the dialog
        val updatedAdapter = SavedProfilesAdapter(
            onProfileClick = { profile ->
                loadProfile(profile)
                dialog.dismiss()
            },
            onDeleteClick = { profile ->
                AlertDialog.Builder(this)
                    .setTitle(R.string.delete_profile)
                    .setMessage(R.string.confirm_delete_profile)
                    .setPositiveButton("Delete") { _, _ ->
                        if (profileConfigManager.deleteProfile(profile.name)) {
                            Toast.makeText(this, R.string.profile_deleted, Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            // Show the dialog again with updated list
                            showLoadProfileDialog()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        recyclerView.adapter = updatedAdapter
        updatedAdapter.submitList(profiles)

        dialog.show()
    }

    private fun loadProfile(profile: ProfileConfiguration) {
        val profileCodeInput = findViewById<TextInputEditText>(R.id.profileCodeInput)
        val chatTitleInput = findViewById<TextInputEditText>(R.id.chatTitleInput)
        val bgColorInput = findViewById<TextInputEditText>(R.id.bgColorInput)
        val actionBarColorInput = findViewById<TextInputEditText>(R.id.actionBarColorInput)
        val sendButtonColorInput = findViewById<TextInputEditText>(R.id.sendButtonColorInput)
        val outgoingBubbleColorInput = findViewById<TextInputEditText>(R.id.outgoingBubbleColorInput)
        val outgoingTextColorInput = findViewById<TextInputEditText>(R.id.outgoingTextColorInput)
        val incomingBubbleColorInput = findViewById<TextInputEditText>(R.id.incomingBubbleColorInput)
        val incomingTextColorInput = findViewById<TextInputEditText>(R.id.incomingTextColorInput)
        val actionBarTextColorInput = findViewById<TextInputEditText>(R.id.actionBarTextColorInput)
        val radioDraft = findViewById<RadioButton>(R.id.radioDraft)
        val radioLive = findViewById<RadioButton>(R.id.radioLive)

        profileCodeInput.setText(profile.profileCode)
        chatTitleInput.setText(profile.chatTitle)
        bgColorInput.setText(profile.bgColor)
        actionBarColorInput.setText(profile.actionBarColor)
        sendButtonColorInput.setText(profile.sendButtonColor)
        outgoingBubbleColorInput.setText(profile.outgoingBubbleColor)
        outgoingTextColorInput.setText(profile.outgoingTextColor)
        incomingBubbleColorInput.setText(profile.incomingBubbleColor)
        incomingTextColorInput.setText(profile.incomingTextColor)
        actionBarTextColorInput.setText(profile.actionBarTextColor)

        if (profile.isDraft) {
            radioDraft.isChecked = true
        } else {
            radioLive.isChecked = true
        }

        Toast.makeText(this, R.string.profile_loaded, Toast.LENGTH_SHORT).show()
    }
}
