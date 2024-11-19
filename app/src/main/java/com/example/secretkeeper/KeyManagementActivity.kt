package com.example.secretkeeper

import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import javax.crypto.spec.SecretKeySpec


class KeyManagementActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KeyManagementScreen(
                onGenerateKeyClick = { generateNewKey() },
                onSetKeyClick = { key -> setExistingKey(key) },
                onExportKeyClick = { exportCurrentKey() }
            )
        }
    }

    private fun generateNewKey() {
        val newKey = KeyManager.generateNewKey()
        KeyManager.saveKey(this, newKey)
        Toast.makeText(this, "New key generated and saved!", Toast.LENGTH_SHORT).show()
    }

    private fun setExistingKey(keyString: String) {
        try {
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            val key = SecretKeySpec(keyBytes, "AES")
            KeyManager.saveKey(this, key)
            Toast.makeText(this, "Key successfully set!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid key format", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportCurrentKey() {
        val currentKey = KeyManager.loadKey(this)
        if (currentKey != null) {
            val keyString = Base64.encodeToString(currentKey.encoded, Base64.DEFAULT)
            Toast.makeText(this, "Current key: $keyString", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "No key found!", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun KeyManagementScreen(
    onGenerateKeyClick: () -> Unit,
    onSetKeyClick: (String) -> Unit,
    onExportKeyClick: () -> Unit
) {
    val keyInput = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onGenerateKeyClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Generate New Key")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = keyInput.value,
            onValueChange = { keyInput.value = it },
            label = { Text("Enter Key") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSetKeyClick(keyInput.value) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Set Existing Key")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onExportKeyClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Export Current Key")
        }
    }
}
