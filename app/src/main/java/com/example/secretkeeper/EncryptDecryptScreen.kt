package com.example.secretkeeper

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EncryptDecryptScreen(
    onEncryptClick: () -> Unit,
    onDecryptClick: () -> Unit,
    onNavigateToKeyManagement: () -> Unit,
    onNavigateToS3: () -> Unit,
    onCreateDummyFile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onEncryptClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Encrypt File")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onDecryptClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Decrypt File")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToKeyManagement, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Manage Keys")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToS3, modifier = Modifier.fillMaxWidth()) {
            Text(text = "S3 Operations")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onCreateDummyFile, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Create Dummy File")
        }
    }
}
