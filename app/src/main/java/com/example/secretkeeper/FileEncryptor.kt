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
    }

    fun encryptFile(inputFile: File, outputFile: File): Boolean {
        return try {
            val key = KeyManager.loadKey(context)
                ?: throw IllegalStateException("No active key available for encryption")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv // Get the generated IV
            outputFile.outputStream().use { fileOut ->
                fileOut.write(iv) // Prepend IV to the output file
                CipherOutputStream(fileOut, cipher).use { cipherOut ->
                    inputFile.inputStream().use { fileIn ->
                        fileIn.copyTo(cipherOut)
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
                fileIn.read(iv) // Extract IV from the file

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
                cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

                // Ensure the file doesn't already exist
                if (outputFile.exists()) {
                    outputFile.delete()
                }

                outputFile.outputStream().use { fileOut ->
                    CipherInputStream(fileIn, cipher).use { cipherIn ->
                        cipherIn.copyTo(fileOut)
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
