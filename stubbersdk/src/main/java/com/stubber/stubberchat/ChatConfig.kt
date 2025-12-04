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
    val actionBarColor: Int = Color.parseColor("#0845a6"),
    val sendButtonColor: Int = Color.parseColor("#DCF415"),
    val chatTitle: String = "Sanlam",
    val outgoingBubbleColor: Int = Color.parseColor("#DCF415"),
    val outgoingTextColor: Int = Color.parseColor("#000000"),
    val incomingBubbleColor: Int = Color.parseColor("#FFFFFF"),
    val incomingTextColor: Int = Color.parseColor("#000000"),
    val actionBarTextColor: Int = Color.parseColor("#FFFFFF")
) : Parcelable
