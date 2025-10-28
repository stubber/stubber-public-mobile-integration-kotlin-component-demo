package com.stubber.stubbersdk.stubberchat

import android.content.Context
import android.content.Intent

object StubberChatSDK {
    private var config: ChatConfig? = null

    /**
     * Initialize the SDK with default configuration
     */
    fun initialize() {
        this.config = Environment
        Environment.printEnvironmentInfo()
    }

    /**
     * Get the current configuration
     */
    internal fun getConfig(): ChatConfig {
        return config ?: throw IllegalStateException(
            "StubberChatSDK not initialized. Call StubberChatSDK.initialize() first."
        )
    }

    /**
     * Launch the chat activity
     */
    fun startChat(context: Context) {
        if (config == null) {
            throw IllegalStateException(
                "StubberChatSDK not initialized. Call StubberChatSDK.initialize() first."
            )
        }
        val intent = Intent(context, StubberChatActivity::class.java)
        context.startActivity(intent)
    }
}