package com.stubber.mobilechat

import android.os.Bundle
import android.view.Menu
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.stubber.mobilechat.databinding.ActivityMainBinding
import com.stubber.stubbersdk.stubberchat.StubberChatSDK

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the Chat SDK
        StubberChatSDK.initialize()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Stubber Chat"

        val chatButton = findViewById<Button>(R.id.chatButton)

        chatButton.setOnClickListener {
            // Launch chat using SDK
            StubberChatSDK.startChat(this)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
}