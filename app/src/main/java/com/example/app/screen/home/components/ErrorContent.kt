package com.example.recuerdago.screens.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.viewmodel.AuthViewModel

@Composable
fun ErrorContent(
    errorMessage: String,
    authViewModel: AuthViewModel,
    primaryColor: Color,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Error: $errorMessage",
            fontSize = 16.sp,
            color = Color(0xFFFF6B6B),
            textAlign = TextAlign.Center
        )
        Button(
            onClick = { authViewModel.retryGetCurrentUser() },
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
        ) {
            Text("Reintentar", color = Color.White)
        }
    }
}