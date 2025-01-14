package com.example.secretkeeper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.secretkeeper.ui.theme.SecretKeeperTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (KeyManager.loadKey(this) == null) {
            val defaultKey = KeyManager.generateNewKey()
            KeyManager.saveKey(this, defaultKey)
            Log.d("MainActivity", "Generated a default AES key.")
        }

        setContent {
            SecretKeeperTheme {
                val navController = rememberNavController()
                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(navController = navController, startDestination = "encryptDecrypt") {
                        addEncryptDecryptScreen(navController)
                        addKeyManagementScreen(navController)
                        addS3Screen(navController)
                    }

                    if (isLoading) {
                        LoadingOverlay()
                    }
                }
            }
        }
    }

    private fun NavGraphBuilder.addEncryptDecryptScreen(navController: NavController) {
        composable("encryptDecrypt") {
            EncryptDecryptScreen(
                onEncryptClick = { selectFileForEncryption() },
                onDecryptClick = { selectEncryptedFileForDecryption() },
                onNavigateToKeyManagement = { navController.navigate("keyManagement") },
                onNavigateToS3 = { navController.navigate("s3Screen") },
                onCreateDummyFile = { createDummyFile() }
            )
        }
    }

    private fun NavGraphBuilder.addKeyManagementScreen(navController: NavController) {
        composable("keyManagement") {
            KeyManagementScreen(
                onGenerateKeyClick = { generateNewKey() },
                onSetKeyClick = { setExistingKey(it) },
                onExportKeyClick = { exportCurrentKey() },
                onBackClick = { navController.popBackStack() }
            )
        }
    }

    private fun NavGraphBuilder.addS3Screen(navController: NavController) {
        composable("s3Screen") {
            S3Screen(
                onUploadFile = { selectFileForUpload() },
                onDownloadFile = { key -> handleFileDownload(key) }
            )
        }
    }

    private fun createDummyFile() {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val randomName = generateRandomString(7)
        val dummyFile = File(downloadsDir, "$randomName.txt")

        try {
            FileOutputStream(dummyFile).use { output ->
                val data = "A".repeat(1024) // 1 KB of data
                repeat(5120) { // Repeat 5120 times to create 5 MB
                    output.write(data.toByteArray())
                }
            }
            showToast("Dummy file created: ${dummyFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Failed to create dummy file")
        }
    }

    private fun generateRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
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

                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val originalFileName = encryptedFile.name
                    .removePrefix("encrypted_")
                    .removeSuffix(".enc")
                val finalOutputFile = File(downloadsDir, "decrypted_${originalFileName}")

                decryptFile(encryptedFile, finalOutputFile)
            } else {
                showToast("No file selected for decryption")
            }
        }

    private val selectFileForUploadLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                uploadFileToS3(uri)
            } else {
                showToast("No file selected for upload")
            }
        }

    private fun selectFileForEncryption() {
        selectFileForEncryptionLauncher.launch("*/*")
    }

    private fun selectEncryptedFileForDecryption() {
        selectEncryptedFileLauncher.launch("*/*")
    }

    private fun selectFileForUpload() {
        selectFileForUploadLauncher.launch("*/*")
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
            isLoading = true
            val fileEncryptor = FileEncryptor(this@MainActivity)
            val success = fileEncryptor.encryptFile(inputFile, encryptedFile)

            withContext(Dispatchers.Main) {
                isLoading = false
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
            isLoading = true
            val fileEncryptor = FileEncryptor(this@MainActivity)
            val success = fileEncryptor.decryptFile(encryptedFile, outputFile)

            withContext(Dispatchers.Main) {
                isLoading = false
                if (success) {
                    showToast("File decrypted successfully! Saved at: ${outputFile.absolutePath}")
                } else {
                    showToast("Decryption failed. Make sure you are decrypting with the same key that was used to encrypt the file.")
                }
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationId = System.currentTimeMillis().toInt()
        val channelId = "upload_download_channel"

        // Creează un manager pentru notificări
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Creează canalul de notificare (necesar pentru Android 8.0+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Upload/Download Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for file upload/download status"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Creează notificarea
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Afișează notificarea
        notificationManager.notify(notificationId, notification)
    }

    private fun uploadFileToS3(uri: Uri) {
        val file = uriToFile(uri) ?: run {
            showToast("Failed to access file for uploading")
            return
        }

        val s3Manager = S3Manager(this)
        CoroutineScope(Dispatchers.IO).launch {
            isLoading = true
            s3Manager.uploadFile(file, file.name) { success, message ->
                CoroutineScope(Dispatchers.Main).launch {
                    isLoading = false
                    showNotification("Upload Push Noto.", "Dummy tekst!")
                    showToast(if (success) "Upload successful!" else "Upload failed: $message")
                }
            }
        }
    }

    private fun handleFileDownload(key: String) {
        val s3Manager = S3Manager(context = this)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val outputFile = File(downloadsDir, key)

        s3Manager.downloadFile(key, outputFile) { success, message ->
            runOnUiThread {
                if (success) {
                    showToast("File downloaded successfully!")
                } else {
                    showToast("File download failed: $message")
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

    private fun generateNewKey() {
        val newKey = KeyManager.generateNewKey()
        KeyManager.saveKey(this, newKey)
        showToast("New key generated and saved!")
    }

    private fun setExistingKey(keyString: String) {
        try {
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            val key = SecretKeySpec(keyBytes, "AES")
            KeyManager.saveKey(this, key)
            showToast("Key successfully set!")
        } catch (e: Exception) {
            showToast("Invalid key format")
        }
    }

    private fun exportCurrentKey() {
        val currentKey = KeyManager.loadKey(this)
        if (currentKey != null) {
            val keyString = Base64.encodeToString(currentKey.encoded, Base64.DEFAULT)

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("MasterKey", keyString)
            clipboard.setPrimaryClip(clip)

            showToast("Key copied to clipboard!")
        } else {
            showToast("No key found!")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
