package com.example.app.screen.grupos.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.app.screen.components.AppBackButton
import com.example.app.screen.components.AppButton

@Composable
fun GrupoMapButtons(
    navController: NavController,
    selectedAddress: String,
    onConfirmClick: () -> Unit,
    onRecenterClick: () -> Unit,
    onBackClick: () -> Unit
) {
    // Vacío por ahora, lo implementaremos después
}