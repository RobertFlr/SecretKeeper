package com.example.secretkeeper

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.anyOrNull
import javax.crypto.SecretKey
import java.util.Base64

class KeyManagerTest {
    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Mock SharedPreferences behavior
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
    }

    @Test
    fun `test generateNewKey returns non-null key`() {
        val newKey: SecretKey = KeyManager.generateNewKey()
        assertThat(newKey).isNotNull()
    }

    @Test
    fun `test save and load key`() {
        val newKey = KeyManager.generateNewKey()
        val keyString = Base64.getEncoder().encodeToString(newKey.encoded)

        // Mock SharedPreferences to store the key
        val keyMap = mutableMapOf<String, String>()
        `when`(mockSharedPreferences.getString(anyString(), anyOrNull())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            keyMap[key]
        }

        `when`(mockEditor.putString(anyString(), anyString())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val value = invocation.arguments[1] as String
            keyMap[key] = value
            mockEditor
        }

        // Save key
        KeyManager.saveKey(mockContext, newKey)

        // Load key
        val loadedKey = KeyManager.loadKey(mockContext)
        assertThat(loadedKey).isNotNull()
        assertThat(loadedKey?.encoded).isEqualTo(newKey.encoded)
    }

    @Test
    fun `test loadKey returns null if no key saved`() {
        `when`(mockSharedPreferences.getString(anyString(), anyString())).thenReturn(null)
        val loadedKey = KeyManager.loadKey(mockContext)
        assertThat(loadedKey).isNull()
    }
}
