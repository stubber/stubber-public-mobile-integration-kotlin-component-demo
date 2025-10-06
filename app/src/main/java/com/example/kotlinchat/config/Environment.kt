package com.example.kotlinchat.config

import android.util.Log
import com.example.kotlinchat.BuildConfig

object Environment {
    // Server URL
    val serverUrl: String = if (BuildConfig.DEBUG) {
        "http://192.168.254.32:6020"
    } else {
        "https://api.stubber.zone:6020"
    }

    // File server URL
    val fileServerUrl: String = if (BuildConfig.DEBUG) {
        "https://app.dev.stubber.com/api/fileserver/file"
    } else {
        "https://app.stubber.com/api/fileserver/file"
    }

    // Profile configuration
    const val profileCode: String = "ROTOBP"
    const val profileUuid: String = "0627d28a-96a7-5a39-a2e7-4c0207f8d3be"

    // Print environment info (for debugging)
    fun printEnvironmentInfo() {
        if (BuildConfig.DEBUG) {
            Log.d("Environment", "=== Environment Configuration ===")
            Log.d("Environment", "Server URL: $serverUrl")
            Log.d("Environment", "File Server URL: $fileServerUrl")
            Log.d("Environment", "Profile Code: $profileCode")
            Log.d("Environment", "Profile UUID: $profileUuid")
            Log.d("Environment", "Debug Mode: ${BuildConfig.DEBUG}")
            Log.d("Environment", "================================")
        }
    }
}
