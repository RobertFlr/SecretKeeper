package com.example.secretkeeper

import android.content.Context
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.*
import com.amazonaws.services.s3.AmazonS3Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class S3Manager(context: Context) {
    private val accessKey = "public"
    private val secretKey = "secret"
    private val bucketName = "secret-keeper-v1"
    private val region = "eu-north-1"

    private val s3Client: AmazonS3Client
    private val transferUtility: TransferUtility

    init {
        val credentials = BasicAWSCredentials(accessKey, secretKey)
        s3Client = AmazonS3Client(credentials)
        transferUtility = TransferUtility.builder()
            .context(context)
            .s3Client(s3Client)
            .transferUtilityOptions(TransferUtilityOptions())
            .build()
    }

    fun uploadFile(file: File, key: String, callback: (Boolean, String?) -> Unit) {
        val uploadObserver = transferUtility.upload(bucketName, key, file)

        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                when (state) {
                    TransferState.COMPLETED -> callback(true, null)
                    TransferState.FAILED -> callback(false, "Upload failed")
                    else -> Unit // Ignore other states
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                // Optional: Progress updates
            }

            override fun onError(id: Int, ex: Exception) {
                callback(false, ex.message)
            }
        })
    }

    fun downloadFile(key: String, outputFile: File, callback: (Boolean, String?) -> Unit) {
        val downloadObserver = transferUtility.download(bucketName, key, outputFile)

        downloadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                when (state) {
                    TransferState.COMPLETED -> callback(true, null)
                    TransferState.FAILED -> callback(false, "Download failed")
                    else -> Unit // Ignore other states
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                // Optional: Implement progress updates here
            }

            override fun onError(id: Int, ex: Exception) {
                callback(false, ex.message)
            }
        })
    }
}
