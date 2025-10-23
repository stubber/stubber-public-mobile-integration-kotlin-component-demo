package com.example.kotlinchat

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlinchat.config.Environment
import com.example.kotlinchat.databinding.ActivityMainBinding
import com.stubber.stubberchatsdk.chat.StubberChatSDK

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the Chat SDK with configuration
        StubberChatSDK.initialize(Environment)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val chatButton = findViewById<Button>(R.id.chatButton)

        chatButton.setOnClickListener {
            // Launch chat using SDK
            StubberChatSDK.startChat(this)
        }
    }
}