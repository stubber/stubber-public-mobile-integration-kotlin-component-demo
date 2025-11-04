# Stubber Chat SDK

A Kotlin Android SDK for integrating Stubber chat functionality into your Android applications.

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Usage](#usage)
- [Customization](#customization)
- [Build Types & Branches](#build-types--branches)
- [Features](#features)
- [Architecture](#architecture)

## Installation

### Option 1: Local Module

1. Copy the `stubbersdk` module into your Android project
2. Add the module to your `settings.gradle.kts`:

```kotlin
include(":app", ":stubbersdk")
```

3. Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":stubbersdk"))
}
```

### Option 2: AAR Library

1. Build the AAR file:
```bash
./gradlew :stubbersdk:assembleRelease
```

2. The AAR will be located at: `stubbersdk/build/outputs/aar/stubbersdk-release.aar`

3. Copy the AAR to your project's `libs` folder and add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(files("libs/stubbersdk-release.aar"))
}
```

## Quick Start

### 1. Create a ChatConfig

```kotlin
import com.stubber.stubbersdk.stubberchat.ChatConfig
import com.stubber.stubbersdk.stubberchat.StubberChatSDK
import android.graphics.Color

// Create configuration
val config = ChatConfig(
    serverUrl = "https://api.stubber.zone:6020",
    fileServerUrl = "https://app.stubber.com/api/fileserver/file",
    profileCode = "YOUR_PROFILE_CODE",
    profileBranch = "draft", // or "live"
    backgroundColor = Color.parseColor("#ECE5DD"),
    primaryColor = Color.parseColor("#25D366"),
    chatTitle = "Support Chat"
)
```

### 2. Launch the Chat

```kotlin
// Start chat activity with configuration
StubberChatSDK.startChat(this, config)
```

That's it! The SDK handles everything else.

## Configuration

### ChatConfig Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `serverUrl` | String | Yes | WebSocket server URL |
| `fileServerUrl` | String | Yes | File server URL for attachments |
| `profileCode` | String | Yes | Your Stubber profile code |
| `profileBranch` | String | Yes | Branch to use: "draft" or "live" |
| `backgroundColor` | Int | No | Chat background color (default: #ECE5DD) |
| `primaryColor` | Int | No | Primary UI color (default: #DCF415) |
| `chatTitle` | String | No | Chat window title (default: "Sanlam") |

### Server URLs

**Release/Production:**
```kotlin
serverUrl = "https://api.stubber.zone:6020"
fileServerUrl = "https://app.stubber.com/api/fileserver/file"
```

### Profile Code

The profile code identifies your Stubber profile configuration. You can find your profile code in the [Stubber management portal](https://manage.stubber.com).

## Usage

### Basic Implementation

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.openChatButton).setOnClickListener {
            openChat()
        }
    }

    private fun openChat() {
        val config = ChatConfig(
            serverUrl = "https://api.stubber.zone:6020",
            fileServerUrl = "https://app.stubber.com/api/fileserver/file",
            profileCode = "YOUR_PROFILE_CODE",
            profileBranch = "draft",
            backgroundColor = Color.parseColor("#ECE5DD"),
            primaryColor = Color.parseColor("#25D366"),
            chatTitle = "Customer Support"
        )

        StubberChatSDK.startChat(this, config)
    }
}
```
## Customization

### Colors

You can customize the chat appearance by setting custom colors:

```kotlin
val config = ChatConfig(
    // ... other params
    backgroundColor = Color.parseColor("#F5F5F5"),  // Light gray background
    primaryColor = Color.parseColor("#007AFF"),      // iOS blue
    chatTitle = "Help Center"
)
```

**What each color controls:**

- **backgroundColor**: Main chat background color
- **primaryColor**:
  - Toolbar background
  - Send button background
  - Outgoing message bubbles
  - Links and accents

### Chat Title

The chat title appears in the toolbar:

```kotlin
val config = ChatConfig(
    // ... other params
    chatTitle = "24/7 Support"
)
```

### Branches

Branches control which version of your Stubber configuration is used:

- **draft**: Test/staging version of your profile configuration
- **live**: Production version of your profile configuration

This allows you to test changes before deploying them to users:

```kotlin
profileBranch = if (BuildConfig.DEBUG) "draft" else "live"
```

Or let users choose:

```kotlin
val isDraft = userPreferences.getBoolean("use_draft_branch", false)
val config = ChatConfig(
    // ... other params
    profileBranch = if (isDraft) "draft" else "live"
)
```
The SDK supports:

1. **Text messages**: Plain text or markdown-formatted
2. **Image messages**: JPG, PNG with inline preview
3. **Audio messages**: Voice notes or audio files with playback controls
4. **Mixed messages**: Text + multiple attachments

### Session Management

The SDK automatically manages user sessions:

- Creates new session on first connection
- Persists session UUID locally
- Restores session on reconnection
- Messages are stored locally and persist across app restarts

## Requirements

- Android API 24+ (Android 7.0)
- Kotlin 1.9+
- AndroidX libraries
- Internet permission

## Permissions

The SDK requires the following permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
``
