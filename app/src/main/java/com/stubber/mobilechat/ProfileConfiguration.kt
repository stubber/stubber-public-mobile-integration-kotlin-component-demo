package com.stubber.mobilechat

import org.json.JSONObject

data class ProfileConfiguration(
    val name: String,
    val profileCode: String,
    val isDraft: Boolean,
    val chatTitle: String,
    val bgColor: String,
    val actionBarColor: String = "#0845a6",
    val sendButtonColor: String = "#DCF415",
    val outgoingBubbleColor: String = "#DCF415",
    val outgoingTextColor: String = "#000000",
    val incomingBubbleColor: String = "#FFFFFF",
    val incomingTextColor: String = "#000000",
    val actionBarTextColor: String = "#FFFFFF"
) {

    fun toJson(): String {
        val json = JSONObject()
        json.put("name", name)
        json.put("profileCode", profileCode)
        json.put("isDraft", isDraft)
        json.put("chatTitle", chatTitle)
        json.put("bgColor", bgColor)
        json.put("actionBarColor", actionBarColor)
        json.put("sendButtonColor", sendButtonColor)
        json.put("outgoingBubbleColor", outgoingBubbleColor)
        json.put("outgoingTextColor", outgoingTextColor)
        json.put("incomingBubbleColor", incomingBubbleColor)
        json.put("incomingTextColor", incomingTextColor)
        json.put("actionBarTextColor", actionBarTextColor)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonString: String): ProfileConfiguration {
            val json = JSONObject(jsonString)
            return ProfileConfiguration(
                name = json.getString("name"),
                profileCode = json.getString("profileCode"),
                isDraft = json.getBoolean("isDraft"),
                chatTitle = json.getString("chatTitle"),
                bgColor = json.getString("bgColor"),
                actionBarColor = json.optString("actionBarColor", json.optString("primaryColor", "#0845a6")),
                sendButtonColor = json.optString("sendButtonColor", json.optString("primaryColor", "#DCF415")),
                outgoingBubbleColor = json.optString("outgoingBubbleColor", "#DCF415"),
                outgoingTextColor = json.optString("outgoingTextColor", "#000000"),
                incomingBubbleColor = json.optString("incomingBubbleColor", "#FFFFFF"),
                incomingTextColor = json.optString("incomingTextColor", "#000000"),
                actionBarTextColor = json.optString("actionBarTextColor", "#FFFFFF")
            )
        }
    }
}