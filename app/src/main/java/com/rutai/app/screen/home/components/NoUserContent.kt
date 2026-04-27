package com.rutai.recuerdago.screens.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.rutai.app.R
import com.rutai.app.viewmodel.AuthViewModel

@Composable
fun NoUserContent(authViewModel: AuthViewModel) {
    val context = LocalContext.current  // ✅ Obtener el contexto

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.no_user_info),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { authViewModel.logout(context) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                stringResource(R.string.settings_logout),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
