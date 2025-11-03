package com.stubber.mobilechat

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.stubber.mobilechat.databinding.ActivityMainBinding
import com.stubber.stubbersdk.stubberchat.ChatConfig
import com.stubber.stubbersdk.stubberchat.StubberChatSDK

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bgColorPreview: View
    private lateinit var primaryColorPreview: View

    // Server configurations
    companion object {
        // Debug build config
        private const val DEBUG_SERVER_URL = "http://192.168.254.32:6020"
        private const val DEBUG_FILE_SERVER_URL = "https://app.dev.stubber.com/api/fileserver/file"

        // Release build config
        private const val RELEASE_SERVER_URL = "https://api.stubber.zone:6020"
        private const val RELEASE_FILE_SERVER_URL = "https://app.stubber.com/api/fileserver/file"

        // SharedPreferences keys
        private const val PREFS_NAME = "stubber_demo_prefs"
        private const val KEY_PROFILE_CODE = "profile_code"
        private const val KEY_CHAT_TITLE = "chat_title"
        private const val KEY_BG_COLOR = "bg_color"
        private const val KEY_PRIMARY_COLOR = "primary_color"
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
        val primaryColorInput = findViewById<TextInputEditText>(R.id.primaryColorInput)
        val openChatButton = findViewById<Button>(R.id.openChatButton)

        bgColorPreview = findViewById(R.id.bgColorPreview)
        primaryColorPreview = findViewById(R.id.primaryColorPreview)

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

        // Update primary color preview as user types
        primaryColorInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val colorString = s.toString().trim()
                if (colorString.isNotEmpty()) {
                    updateColorPreview(primaryColorPreview, colorString)
                } else {
                    updateColorPreview(primaryColorPreview, "#DCF415")
                }
            }
        })

        val radioDraft = findViewById<RadioButton>(R.id.radioDraft)

        openChatButton.setOnClickListener {
            val profileCode = profileCodeInput.text.toString().trim()
            val chatTitle = chatTitleInput.text.toString().trim()
            val bgColorString = bgColorInput.text.toString().trim()
            val primaryColorString = primaryColorInput.text.toString().trim()
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

            // Parse primary color or use default if empty
            val primaryColor = if (primaryColorString.isEmpty()) {
                Color.parseColor("#DCF415")
            } else {
                try {
                    Color.parseColor(primaryColorString)
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(this, "Invalid primary color format. Use hex color (e.g. #DCF415)", Toast.LENGTH_SHORT).show()
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
                primaryColor = primaryColor,
                chatTitle = title
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
        val primaryColorInput = findViewById<TextInputEditText>(R.id.primaryColorInput)
        val radioDraft = findViewById<RadioButton>(R.id.radioDraft)
        val radioLive = findViewById<RadioButton>(R.id.radioLive)

        // Load saved values
        profileCodeInput.setText(prefs.getString(KEY_PROFILE_CODE, ""))
        chatTitleInput.setText(prefs.getString(KEY_CHAT_TITLE, ""))
        bgColorInput.setText(prefs.getString(KEY_BG_COLOR, ""))
        primaryColorInput.setText(prefs.getString(KEY_PRIMARY_COLOR, ""))

        val isDraft = prefs.getBoolean(KEY_IS_DRAFT, true)
        if (isDraft) {
            radioDraft.isChecked = true
        } else {
            radioLive.isChecked = true
        }

        // Update color previews
        val bgColor = prefs.getString(KEY_BG_COLOR, "")
        val primaryColor = prefs.getString(KEY_PRIMARY_COLOR, "")

        if (bgColor.isNullOrEmpty()) {
            updateColorPreview(bgColorPreview, "#ECE5DD")
        } else {
            updateColorPreview(bgColorPreview, bgColor)
        }

        if (primaryColor.isNullOrEmpty()) {
            updateColorPreview(primaryColorPreview, "#DCF415")
        } else {
            updateColorPreview(primaryColorPreview, primaryColor)
        }
    }

    private fun saveValues() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()

        val profileCodeInput = findViewById<TextInputEditText>(R.id.profileCodeInput)
        val chatTitleInput = findViewById<TextInputEditText>(R.id.chatTitleInput)
        val bgColorInput = findViewById<TextInputEditText>(R.id.bgColorInput)
        val primaryColorInput = findViewById<TextInputEditText>(R.id.primaryColorInput)
        val radioDraft = findViewById<RadioButton>(R.id.radioDraft)

        editor.putString(KEY_PROFILE_CODE, profileCodeInput.text.toString())
        editor.putString(KEY_CHAT_TITLE, chatTitleInput.text.toString())
        editor.putString(KEY_BG_COLOR, bgColorInput.text.toString())
        editor.putString(KEY_PRIMARY_COLOR, primaryColorInput.text.toString())
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
}