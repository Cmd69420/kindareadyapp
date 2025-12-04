package com.bluemix.clients_lead.features.auth.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import ui.AppTheme
import ui.components.Text

@Composable
fun PasswordHintRow(password: String) {
    val strength = remember(password) { passwordStrength(password) }
    val text = when (strength) {
        0 -> "Password too short"
        1 -> "Weak password"
        2 -> "Okay"
        else -> "Strong password"
    }
    val alpha = when (strength) {
        0 -> 1f
        1 -> .9f
        2 -> .8f
        else -> .8f
    }
    Text(
        text,
        style = AppTheme.typography.label2,
        modifier = Modifier
            .padding(top = 6.dp)
            .alpha(alpha)
    )
}

fun passwordStrength(pw: String): Int {
    var score = 0
    if (pw.length >= 6) score++
    if (pw.length >= 10) score++
    val hasMix = pw.any { it.isDigit() } && pw.any { it.isLetter() }
    if (hasMix) score++
    return score.coerceIn(0, 3)
}