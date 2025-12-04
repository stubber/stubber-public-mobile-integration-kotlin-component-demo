package com.stubber.mobilechat

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class ProfileConfigurationManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("profile_configurations", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "ProfileConfigManager"
        private const val KEY_PROFILE_LIST = "profile_list"
        private const val KEY_PROFILE_PREFIX = "profile_"
    }

    fun saveProfile(profile: ProfileConfiguration): Boolean {
        return try {
            // Save the profile data
            prefs.edit()
                .putString(KEY_PROFILE_PREFIX + profile.name, profile.toJson())
                .apply()

            // Update the profile list
            val profiles = getProfileNames().toMutableSet()
            profiles.add(profile.name)
            prefs.edit()
                .putStringSet(KEY_PROFILE_LIST, profiles)
                .apply()

            Log.d(TAG, "Saved profile: ${profile.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving profile", e)
            false
        }
    }

    fun loadProfile(name: String): ProfileConfiguration? {
        return try {
            val jsonString = prefs.getString(KEY_PROFILE_PREFIX + name, null)
            if (jsonString != null) {
                ProfileConfiguration.fromJson(jsonString)
            } else {
                Log.w(TAG, "Profile not found: $name")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profile", e)
            null
        }
    }

    fun deleteProfile(name: String): Boolean {
        return try {
            // Remove the profile data
            prefs.edit()
                .remove(KEY_PROFILE_PREFIX + name)
                .apply()

            // Update the profile list
            val profiles = getProfileNames().toMutableSet()
            profiles.remove(name)
            prefs.edit()
                .putStringSet(KEY_PROFILE_LIST, profiles)
                .apply()

            Log.d(TAG, "Deleted profile: $name")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting profile", e)
            false
        }
    }

    fun getProfileNames(): List<String> {
        return try {
            prefs.getStringSet(KEY_PROFILE_LIST, emptySet())?.toList()?.sorted() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting profile names", e)
            emptyList()
        }
    }

    fun profileExists(name: String): Boolean {
        return prefs.contains(KEY_PROFILE_PREFIX + name)
    }
}