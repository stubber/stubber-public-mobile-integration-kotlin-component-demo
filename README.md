## Configuration

### Profile UUID

The `profileUuid` value in `app/src/main/java/com/example/kotlinchat/config/Environment.kt:16` is where you configure your specific profile UUID.

To find or manage your profile UUID:

1. Visit the [Stubber profile management page](https://manage.stubber.com/sanlam/configs/notifications/mobile_app/profiles/c8e70d1a-145f-5a22-9d4b-beb7b66a0a99)
2. Copy your profile UUID
3. Update the `profileUuid` value in `Environment.kt`

### Environment Settings

The `Environment.kt` file contains additional configuration options:

- **profileBranch**: A string value that targets either the "draft" or "live" stub configured in your profile. Automatically set to "draft" in debug builds and "live" in release builds.

### Architecture

The MainActivity is just for demo purposes. All the chat logic is contained in the ChatActivity.

The ChatService contains all the Socket.IO and message storage logic.

#### Resetting a User Session

The chat activity has built-in functions to handle session resets:

- **`clearSession`**: Deletes a session and exits the activity
- **`reloadConnection`**: Deletes a session and reconnects to the server so the user can chat in a new thread

**To Manually reset a user's session and exit:**
1. Call `chatService.clearSession()`
2. Call `messageViewModel.clearMessages()`
3. Exit the activity

**To Manually reset a user's session without exiting:**
1. Call `chatService.disconnect()`
2. Call `chatService.connect()`
