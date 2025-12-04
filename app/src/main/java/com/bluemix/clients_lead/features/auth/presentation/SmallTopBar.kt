package com.bluemix.clients_lead.features.auth.presentation

import android.util.Patterns
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.AppTheme
import ui.components.Icon
import ui.components.IconButton
import ui.components.IconButtonVariant
import ui.components.Text

@Composable
fun SmallTopBar(
    title: String,
    onBack: () -> Unit
) {
    LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = onBack, variant = IconButtonVariant.Ghost) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = null)
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = title,
            style = AppTheme.typography.h2,
        )
        Spacer(Modifier.weight(1f))
        // Keep layout balanced
        Spacer(Modifier.width(64.dp))
    }
}

fun String.isValidEmail(): Boolean =
    isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(this).matches()
