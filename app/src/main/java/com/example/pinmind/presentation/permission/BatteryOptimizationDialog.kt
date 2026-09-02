package com.example.pinmind.presentation.permission

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.pinmind.R
import com.example.pinmind.core.util.BatteryOptimizationHelper

/**
 * Dialog prompting user to disable battery optimizations for PinMind.
 */
@Composable
fun BatteryOptimizationDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.BatteryAlert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.battery_opt_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(R.string.battery_opt_desc),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    requestIgnoreBatteryOptimization(context)
                }
            ) {
                Text(text = stringResource(R.string.battery_opt_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.battery_opt_dismiss))
            }
        },
        modifier = modifier
    )
}

private fun requestIgnoreBatteryOptimization(context: Context) {
    try {
        val intent = BatteryOptimizationHelper.createIgnoreBatteryOptimizationIntent(context)
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback or ignore if not supported by OEM
    }
}
