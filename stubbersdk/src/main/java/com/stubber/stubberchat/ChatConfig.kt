package com.stubber.stubbersdk.stubberchat

import android.graphics.Color
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatConfig(
    val serverUrl: String,
    val fileServerUrl: String,
    val profileCode: String,
    val profileBranch: String,
    val backgroundColor: Int = Color.parseColor("#ECE5DD"),
    val primaryColor: Int = Color.parseColor("#DCF415"),
    val chatTitle: String = "Sanlam"
) : Parcelable
