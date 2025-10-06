package com.example.kotlinchat.config

import android.util.Log
import com.example.kotlinchat.BuildConfig

object Environment {
    // Dev config
    // val serverUrl: String = "http://192.168.254.32:6020"
    // val fileServerUrl: String = "https://app.dev.stubber.com/api/fileserver/file" 
    // val profileCode: String = ""
    // val profileUuid: String = "0627d28a-96a7-5a39-a2e7-4c0207f8d3be"

    // Sanlum config
    val serverUrl: String = "https://api.stubber.zone:6020"
    val fileServerUrl: String = "https://app.stubber.com/api/fileserver/file" 
    val profileCode: String = ""
    val profileUuid: String = "c8e70d1a-145f-5a22-9d4b-beb7b66a0a99"


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
            // Log.d("Environment", "File Server URL: $fileServerUrl")
            Log.d("Environment", "Profile Code: $profileCode")
            Log.d("Environment", "Profile UUID: $profileUuid")
            Log.d("Environment", "Profile branch: $profileBranch")
            Log.d("Environment", "Debug Mode: ${BuildConfig.DEBUG}")
            Log.d("Environment", "================================")
        }
    }
}
