package com.example.secretkeeper

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.io.File
import io.mockk.*

class S3ManagerTest {

    private lateinit var mockS3Manager: S3Manager

    @Before
    fun setup() {
        mockS3Manager = mockk()
    }

    @Test
    fun `test downloadFile success`() {
        val fileKey = "testKey"
        val outputFile = File.createTempFile("download", ".txt")

        every { mockS3Manager.downloadFile(fileKey, outputFile, any()) } answers {
            thirdArg<(Boolean, String?) -> Unit>().invoke(true, null)
        }

        var result: Boolean? = null
        mockS3Manager.downloadFile(fileKey, outputFile) { success, _ ->
            result = success
        }

        assertThat(result).isTrue()
    }

    @Test
    fun `test uploadFile success`() {
        val file = File.createTempFile("test", ".txt")
        file.writeText("Test Upload")

        every { mockS3Manager.uploadFile(file, "testKey", any()) } answers {
            thirdArg<(Boolean, String?) -> Unit>().invoke(true, null)
        }

        var result: Boolean? = null
        mockS3Manager.uploadFile(file, "testKey") { success, _ ->
            result = success
        }

        assertThat(result).isTrue()
    }
}
