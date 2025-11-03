package com.stubber.stubbersdk.stubberchat

import android.content.Context
import android.content.Intent

object StubberChatSDK {
    const val EXTRA_CHAT_CONFIG = "extra_chat_config"

    /**
     * Launch the chat activity with the provided configuration
     */
    fun startChat(context: Context, config: ChatConfig) {
        val intent = Intent(context, StubberChatActivity::class.java)
        intent.putExtra(EXTRA_CHAT_CONFIG, config)
        context.startActivity(intent)
    }
}