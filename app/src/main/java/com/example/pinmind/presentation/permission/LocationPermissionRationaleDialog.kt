package com.example.pinmind.presentation.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pinmind.R
import com.example.pinmind.core.location.LocationPermissionHelper
import com.example.pinmind.core.location.LocationPermissionState

/**
 * Two-step permission handler complying with Android 10+ location policies.
 *
 * Flow:
 * 1. Explains foreground access with in-app UI -> Requests FINE & COARSE location.
 * 2. Only after foreground is granted -> Explains background "Allow all the time" access -> Requests BACKGROUND location.
 */
@Composable
fun LocationPermissionFlow(
    onPermissionStateChanged: (LocationPermissionState) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember {
        val initial = LocationPermissionHelper.getPermissionState(context)
        mutableStateOf(
            when (initial) {
                LocationPermissionState.Denied -> PermissionStep.ForegroundRationale
                LocationPermissionState.ForegroundOnly -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    PermissionStep.BackgroundRationale
                } else {
                    PermissionStep.Completed
                }
                LocationPermissionState.GrantedAllTime -> PermissionStep.Completed
            }
        )
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val newState = LocationPermissionHelper.getPermissionState(context)
        onPermissionStateChanged(newState)
        onDismiss()
    }

    val foregroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isFineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val isCoarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (isFineGranted || isCoarseGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                currentStep = PermissionStep.BackgroundRationale
            } else {
                onPermissionStateChanged(LocationPermissionState.GrantedAllTime)
                onDismiss()
            }
        } else {
            onPermissionStateChanged(LocationPermissionState.Denied)
            onDismiss()
        }
    }

    when (currentStep) {
        PermissionStep.ForegroundRationale -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.perm_foreground_title),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.perm_foreground_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            foregroundPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.perm_foreground_button))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        PermissionStep.BackgroundRationale -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.perm_background_title),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.perm_background_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        // On Android 11+, system requires directing user to App Settings for background location
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                        onDismiss()
                                    } else {
                                        backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.perm_background_button))
                        }

                        OutlinedButton(
                            onClick = {
                                onPermissionStateChanged(LocationPermissionState.ForegroundOnly)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.perm_background_skip))
                        }
                    }
                }
            )
        }

        PermissionStep.Completed -> {
            // No dialog needed
        }
    }
}

private enum class PermissionStep {
    ForegroundRationale,
    BackgroundRationale,
    Completed
}
