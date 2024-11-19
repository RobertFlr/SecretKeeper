package com.example.secretkeeper

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.secretkeeper.ui.theme.SecretKeeperTheme
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecretKeeperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onEncryptClick = { selectFileForEncryption() },
                        onDecryptClick = { selectEncryptedFileForDecryption() }
                    )
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

                val fileEncryptor = FileEncryptor(this)
                if (fileEncryptor.decryptFile(encryptedFile, finalOutputFile)) {
                    showToast("File decrypted successfully! Saved at: ${finalOutputFile.absolutePath}")
                } else {
                    showToast("Decryption failed.")
                }
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

        Log.d("EncryptFile", "Input file name: ${inputFile.name}")
        Log.d("EncryptFile", "Encrypted file name: ${encryptedFile.name}")
        val fileEncryptor = FileEncryptor(this)
        if (fileEncryptor.encryptFile(inputFile, encryptedFile)) {
            showToast("File encrypted successfully! Saved at: ${encryptedFile.absolutePath}")
        } else {
            showToast("Encryption failed.")
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onEncryptClick: () -> Unit,
    onDecryptClick: () -> Unit
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
    }
}
