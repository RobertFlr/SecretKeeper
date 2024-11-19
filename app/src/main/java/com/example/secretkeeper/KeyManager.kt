package com.example.secretkeeper

import android.content.Context
import android.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object KeyManager {

    private const val PREFS_NAME = "SecretKeeperPrefs"
    private const val KEY_NAME = "ActiveAESKey"

    // Create new key
    fun generateNewKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }

    // Save active key
    fun saveKey(context: Context, key: SecretKey) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val keyEncoded = Base64.encodeToString(key.encoded, Base64.DEFAULT)
        prefs.edit().putString(KEY_NAME, keyEncoded).apply()
    }

    // Load active key
    fun loadKey(context: Context): SecretKey? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val keyEncoded = prefs.getString(KEY_NAME, null) ?: return null
        val keyBytes = Base64.decode(keyEncoded, Base64.DEFAULT)
        return SecretKeySpec(keyBytes, "AES")
    }

    // Delete active key
    fun clearKey(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_NAME).apply()
    }
}
