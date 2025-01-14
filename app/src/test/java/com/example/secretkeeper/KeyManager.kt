package com.example.secretkeeper

import android.content.Context
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

object KeyManager {
    private const val PREFERENCE_NAME = "KeyManagerPrefs"
    private const val KEY_NAME = "SecretKey"

    fun generateNewKey(): SecretKey {
        val keyGen = javax.crypto.KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }

    fun saveKey(context: Context, key: SecretKey) {
        val prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val keyString = Base64.getEncoder().encodeToString(key.encoded)
        editor.putString(KEY_NAME, keyString).apply()
    }

    fun loadKey(context: Context): SecretKey? {
        val prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val keyString = prefs.getString(KEY_NAME, null) ?: return null
        val keyBytes = Base64.getDecoder().decode(keyString)
        return SecretKeySpec(keyBytes, "AES")
    }
}
