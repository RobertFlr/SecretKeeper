package com.example.secretkeeper

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


@Composable
fun S3Screen(
    onUploadFile: () -> Unit,
    onDownloadFile: (String) -> Unit
) {
    val context = LocalContext.current
    var fileKey by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upload File Button
        Button(
            onClick = {
                onUploadFile() // Trigger file selection in MainActivity
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Upload File to S3")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input for File Key to Download
        TextField(
            value = fileKey,
            onValueChange = { fileKey = it },
            label = { Text(text = "Enter File Key to Download") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Download File Button
        Button(
            onClick = {
                if (fileKey.isNotEmpty()) {
                    onDownloadFile(fileKey) // Trigger the download logic in MainActivity
                } else {
                    Toast.makeText(context, "Please enter a valid file key.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Download File from S3")
        }
    }
}
