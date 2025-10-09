package com.example.app.screen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    outlined: Boolean = false,
    enabled: Boolean = true,
    disabledContainerColor: Color? = null,
    disabledContentColor: Color? = null
) {
    val buttonColors = if (outlined) {
        ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = disabledContentColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            containerColor = Color.Transparent
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = disabledContainerColor ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            disabledContentColor = disabledContentColor ?: MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        )
    }

    val shape = RoundedCornerShape(16.dp)
    val border = if (outlined) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    val buttonModifier = modifier
        .fillMaxWidth()
        .height(56.dp)

    val content: @Composable RowScope.() -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }


    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            shape = shape,
            colors = buttonColors,
            border = border,
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
            content = content
        )
    } else {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            shape = shape,
            colors = buttonColors,
            enabled = enabled && !isLoading,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (enabled) 6.dp else 2.dp,
                pressedElevation = if (enabled) 2.dp else 0.dp
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
            content = content
        )
    }
}
