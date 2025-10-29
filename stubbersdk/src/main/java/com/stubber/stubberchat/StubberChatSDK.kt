package com.stubber.stubbersdk.stubberchat

import android.content.Context
import android.content.Intent

object StubberChatSDK {
    /**
     * Get the current configuration
     */
    internal fun getConfig(): ChatConfig {
        return Environment
    }

    /**
     * Launch the chat activity
     */
    fun startChat(context: Context) {
        val intent = Intent(context, StubberChatActivity::class.java)
        context.startActivity(intent)
    }
}