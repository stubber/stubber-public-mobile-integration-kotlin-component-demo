package com.stubber.stubbersdk.stubberchat.models

import org.json.JSONObject

data class IncomingPayload(
    val sessionUuid: String?,
    val stubSession: StubSession?,
    val mobileMessage: MobileMessage?
) {
    companion object {
        fun fromJson(json: JSONObject): IncomingPayload {
            val stubSessionJson = json.optJSONObject("stubsession")
            val mobileMessageJson = json.optJSONObject("mobile_message")

            return IncomingPayload(
                sessionUuid = json.optString("sessionuuid", null),
                stubSession = stubSessionJson?.let { StubSession.fromJson(it) },
                mobileMessage = mobileMessageJson?.let { MobileMessage.fromJson(it) }
            )
        }
    }
}
