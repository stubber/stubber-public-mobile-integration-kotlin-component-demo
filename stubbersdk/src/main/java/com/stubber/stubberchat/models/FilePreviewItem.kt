package com.stubber.stubbersdk.stubberchat.models

import android.net.Uri

data class FilePreviewItem(
    val uri: Uri,
    val attachment: Attachment? = null
)
