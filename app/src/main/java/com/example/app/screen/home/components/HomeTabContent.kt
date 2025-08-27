package com.example.app.screen.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.app.viewmodel.AuthViewModel
import com.example.recuerdago.screens.tabs.ErrorContent
import com.example.recuerdago.screens.tabs.LoadingContent
import com.example.recuerdago.screens.tabs.ModulesSection
import com.example.recuerdago.screens.tabs.NoUserContent
import com.example.recuerdago.screens.tabs.UserWelcomeContent

@Composable
fun HomeTabContent(
    userState: Any?,
    isLoading: Boolean,
    errorMessage: String?,
    authViewModel: AuthViewModel,
    primaryColor: Color,
    textColor: Color,
    cardColors: CardColors,
    showContent: Boolean,
    accentColor: Color,
    isDarkTheme: Boolean,
    onTabSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = showContent,
            enter = slideInVertically(
                initialOffsetY = { -it }
            ) + fadeIn()
        ) {
            when {
                isLoading -> {
                    LoadingContent(primaryColor)
                }
                userState != null -> {
                    UserWelcomeContent(userState, textColor, primaryColor, cardColors)
                }
                errorMessage != null -> {
                    ErrorContent(errorMessage, authViewModel, primaryColor, textColor)
                }
                else -> {
                    NoUserContent(authViewModel, primaryColor, textColor)
                }
            }
        }

        AnimatedVisibility(
            visible = showContent && userState != null,
            enter = slideInVertically(
                initialOffsetY = { it }
            ) + fadeIn()
        ) {
            val currentUserId = try {
                userState!!::class.java.getDeclaredField("id").apply { isAccessible = true }.get(userState).toString()
            } catch (e: Exception) { "N/A" }

            ModulesSection(
                userId = currentUserId,
                primaryColor = primaryColor,
                accentColor = accentColor,
                cardColors = cardColors,
                isDarkTheme = isDarkTheme,
                onTabSelected = onTabSelected
            )
        }
    }
}