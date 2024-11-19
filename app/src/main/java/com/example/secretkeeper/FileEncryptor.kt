package com.example.secretkeeper

import android.content.Context
import android.util.Log
import java.io.File
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec

class FileEncryptor(private val context: Context) {

    companion object {
        private const val TAG = "FileEncryptor"
        private const val IV_SIZE = 12 // GCM recommended IV size
        private const val TAG_SIZE = 128 // GCM authentication tag size in bits
        private const val BUFFER_SIZE = 8192 // 8 KB buffer for streaming
    }

    fun encryptFile(inputFile: File, outputFile: File): Boolean {
        return try {
            val key = KeyManager.loadKey(context)
                ?: throw IllegalStateException("No active key available for encryption")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv
            outputFile.outputStream().use { fileOut ->
                fileOut.write(iv) // Write IV to the output file
                inputFile.inputStream().use { fileIn ->
                    CipherOutputStream(fileOut, cipher).buffered(BUFFER_SIZE).use { cipherOut ->
                        fileIn.copyTo(cipherOut, BUFFER_SIZE)
                    }
                }
            }

            Log.d(TAG, "File encrypted successfully: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun decryptFile(encryptedFile: File, outputFile: File): Boolean {
        return try {
            val key = KeyManager.loadKey(context)
                ?: throw IllegalStateException("No active key available for decryption")

            encryptedFile.inputStream().use { fileIn ->
                val iv = ByteArray(IV_SIZE)
                fileIn.read(iv) // Read IV from the input file

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
                cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

                outputFile.outputStream().apply {
                    channel.truncate(0) // Ensure the output file is empty
                }.use { fileOut ->
                    CipherInputStream(fileIn, cipher).buffered(BUFFER_SIZE).use { cipherIn ->
                        cipherIn.copyTo(fileOut, BUFFER_SIZE)
                    }
                }
            }

            Log.d(TAG, "File decrypted successfully: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}
