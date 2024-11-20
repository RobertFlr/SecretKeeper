package com.example.secretkeeper

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun KeyManagementScreen(
    onGenerateKeyClick: () -> Unit,
    onSetKeyClick: (String) -> Unit,
    onExportKeyClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var keyInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onGenerateKeyClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Generate New Key")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("Enter Key") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { onSetKeyClick(keyInput) }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Set Key")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onExportKeyClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Export Key")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Back")
        }
    }
}
