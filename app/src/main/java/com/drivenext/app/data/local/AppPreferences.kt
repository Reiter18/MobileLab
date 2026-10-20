package com.drivenext.app.data.local

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    private companion object {
        const val FILE_NAME = "drivenext_prefs"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_ACCESS_TOKEN = "access_token"
    }
}
