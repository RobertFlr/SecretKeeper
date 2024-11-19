package com.example.secretkeeper

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.secretkeeper.ui.theme.SecretKeeperTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    private var isLoading by mutableStateOf(false) // State for loading animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for active key, create one if there is none
        if (KeyManager.loadKey(this) == null) {
            val defaultKey = KeyManager.generateNewKey()
            KeyManager.saveKey(this, defaultKey)
            Log.d("MainActivity", "Generated a default AES key.")
        }

        setContent {
            SecretKeeperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainScreen(
                            modifier = Modifier.padding(innerPadding),
                            onEncryptClick = { selectFileForEncryption() },
                            onDecryptClick = { selectEncryptedFileForDecryption() },
                            onManageKeysClick = { openKeyManagement() }
                        )

                        if (isLoading) {
                            LoadingOverlay()
                        }
                    }
                }
            }
        }
    }

    private val selectFileForEncryptionLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                encryptFile(uri)
            } else {
                showToast("No file selected for encryption")
            }
        }

    private val selectEncryptedFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val encryptedFile = uriToFile(uri) ?: run {
                    showToast("Failed to access encrypted file")
                    return@registerForActivityResult
                }

                Log.d("DecryptFile", "Encrypted file name: ${encryptedFile.name}")
                if (!encryptedFile.name.endsWith(".enc")) {
                    showToast("Selected file is not encrypted.")
                    return@registerForActivityResult
                }

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val originalFileName = encryptedFile.name
                    .removePrefix("encrypted_")
                    .removeSuffix(".enc")
                val finalOutputFile = File(downloadsDir, "decrypted_${originalFileName}")

                decryptFile(encryptedFile, finalOutputFile)
            } else {
                showToast("No file selected for decryption")
            }
        }

    private fun selectFileForEncryption() {
        selectFileForEncryptionLauncher.launch("*/*")
    }

    private fun selectEncryptedFileForDecryption() {
        selectEncryptedFileLauncher.launch("*/*")
    }

    private fun encryptFile(uri: Uri) {
        val inputFile = uriToFile(uri) ?: run {
            showToast("Failed to access file for encryption")
            return
        }

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val inputFileName = inputFile.nameWithoutExtension
        val inputFileExtension = inputFile.extension
        val encryptedFile = File(downloadsDir, "encrypted_${inputFileName}.${inputFileExtension}.enc")

        CoroutineScope(Dispatchers.IO).launch {
            isLoading = true // Start loading animation
            val fileEncryptor = FileEncryptor(this@MainActivity)
            val success = fileEncryptor.encryptFile(inputFile, encryptedFile)

            withContext(Dispatchers.Main) {
                isLoading = false // Stop loading animation
                if (success) {
                    showToast("File encrypted successfully! Saved at: ${encryptedFile.absolutePath}")
                } else {
                    showToast("Encryption failed.")
                }
            }
        }
    }

    private fun decryptFile(encryptedFile: File, outputFile: File) {
        CoroutineScope(Dispatchers.IO).launch {
            isLoading = true // Start loading animation
            val fileEncryptor = FileEncryptor(this@MainActivity)
            val success = fileEncryptor.decryptFile(encryptedFile, outputFile)

            withContext(Dispatchers.Main) {
                isLoading = false // Stop loading animation
                if (success) {
                    showToast("File decrypted successfully! Saved at: ${outputFile.absolutePath}")
                } else {
                    showToast("Decryption failed. Make sure you are decrypting with the same key that was used to encrypt the file.")
                }
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = getFileName(uri) ?: "tempfile"
            val tempFile = File(cacheDir, fileName)
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("_display_name")
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    }

    private fun openKeyManagement() {
        val intent = Intent(this, KeyManagementActivity::class.java)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onEncryptClick: () -> Unit,
    onDecryptClick: () -> Unit,
    onManageKeysClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onEncryptClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Encrypt File")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDecryptClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Decrypt File")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onManageKeysClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Manage Keys")
        }
    }
}

@Composable
fun LoadingOverlay() {
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color(0x80000000), shape = androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}
