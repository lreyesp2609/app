package com.rutai.app.screen.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import android.util.Log
import com.rutai.app.BuildConfig
import com.rutai.app.R
import com.rutai.app.utils.SessionManager
import com.rutai.recuerdago.screens.tabs.ModuleCard

@Composable
fun ModulesSection(
    userId: String,
    accentColor: Color,
    onTabSelected: (Int) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.modules_available),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        item {
            ModuleCard(
                title = stringResource(R.string.nav_routes),
                description = stringResource(R.string.feature_save_destinations_desc),
                icon = Icons.Default.Map,
                onClick = { onTabSelected(1) }
            )
        }

        item {
            ModuleCard(
                title = stringResource(R.string.nav_reminders),
                description = stringResource(R.string.feature_datetime_desc),
                icon = Icons.Default.Notifications,
                onClick = { onTabSelected(2) }
            )
        }

        item {
            ModuleCard(
                title = stringResource(R.string.nav_groups),
                description = stringResource(R.string.feature_groups_desc),
                icon = Icons.Default.Group,
                onClick = { onTabSelected(3) }
            )
        }

        if (BuildConfig.DEBUG) {
            item {
                ModuleCard(
                    title = stringResource(R.string.debug_invalidate_token_title),
                    description = stringResource(R.string.debug_invalidate_token_desc),
                    icon = Icons.Default.Warning,
                    onClick = {
                        val sessionManager = SessionManager.getInstance(context)
                        val refreshToken = sessionManager.getRefreshToken()

                        if (!refreshToken.isNullOrEmpty()) {
                            sessionManager.saveTokens("TOKEN_INVALIDO_DEBUG", refreshToken)
                            Log.w("ModulesSection", "🧪 Token de acceso invalidado manualmente para probar auto-refresh")
                        } else {
                            Log.w("ModulesSection", "⚠️ No hay refresh token disponible para prueba de auto-refresh")
                        }
                    }
                )
            }
        }
    }
}
