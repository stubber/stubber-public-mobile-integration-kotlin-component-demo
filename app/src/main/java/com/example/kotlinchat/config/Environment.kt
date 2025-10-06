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
    // const val profileCode: String = "ROTOBP"

    val profileUuid: String = if (BuildConfig.DEBUG) {
        "0627d28a-96a7-5a39-a2e7-4c0207f8d3be"
    } else {
        "fb7686c4-c56a-500a-9b82-1cdd1eccf8b9"
    }

    val profileBranch: String = if (BuildConfig.DEBUG) {
        "draft"
    } else {
        "live"
    }

    // Print environment info (for debugging)
    fun printEnvironmentInfo() {
        if (BuildConfig.DEBUG) {
            Log.d("Environment", "=== Environment Configuration ===")
            Log.d("Environment", "Server URL: $serverUrl")
            Log.d("Environment", "File Server URL: $fileServerUrl")
            // Log.d("Environment", "Profile Code: $profileCode")
            Log.d("Environment", "Profile UUID: $profileBranch")
            Log.d("Environment", "Profile branch: $")
            Log.d("Environment", "Debug Mode: ${BuildConfig.DEBUG}")
            Log.d("Environment", "================================")
        }
    }
}
