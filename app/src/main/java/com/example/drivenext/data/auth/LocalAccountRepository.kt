package com.example.drivenext.data.auth

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

class LocalAccountRepository(context: Context) {
    private val preferences = context.getSharedPreferences("local_account", Context.MODE_PRIVATE)

    fun hasSession(): Boolean = preferences.getBoolean("session", false)

    fun register(email: String, password: String) {
        val random = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = random.joinToString("") { "%02x".format(it) }
        preferences.edit()
            .putString("email", email.trim())
            .putString("salt", salt)
            .putString("password_hash", hash(salt + password))
            .putBoolean("session", true)
            .apply()
    }

    fun signIn(email: String, password: String): Boolean {
        val salt = preferences.getString("salt", null) ?: return false
        val matches = email.trim() == preferences.getString("email", null) &&
            hash(salt + password) == preferences.getString("password_hash", null)
        if (matches) preferences.edit().putBoolean("session", true).apply()
        return matches
    }

    fun signOut() { preferences.edit().remove("session").apply() }

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
