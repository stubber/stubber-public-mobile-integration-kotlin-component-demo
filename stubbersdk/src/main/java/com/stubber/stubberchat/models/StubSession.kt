package com.stubber.stubbersdk.stubberchat.models

import org.json.JSONObject

data class StubSession(
    val setNewWithTimeoutHours: Int
) {
    companion object {
        fun fromJson(json: JSONObject): StubSession {
            return StubSession(
                setNewWithTimeoutHours = json.optInt("set_new_with_timeout_hours", 24)
            )
        }
    }
}
