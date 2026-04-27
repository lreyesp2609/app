package com.rutai.app.screen.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.rutai.app.R
import com.rutai.app.models.User

@Composable
fun UserWelcomeContent(userState: Any?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 0.dp), // ✨ Sin padding vertical
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val userName = (userState as? User)?.let { "${it.nombre} ${it.apellido}" } ?: stringResource(R.string.default_user_name)

        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Person,
            contentDescription = stringResource(R.string.cd_user_icon),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(bottom = 6.dp) // ✨ Reducido a 6dp
                .height(36.dp) // ✨ Reducido a 36dp
        )

        Text(
            text = stringResource(R.string.welcome_user, userName),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.welcome_back_message),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}