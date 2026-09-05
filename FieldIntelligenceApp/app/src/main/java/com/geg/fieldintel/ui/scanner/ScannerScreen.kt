package com.geg.fieldintel.ui.scanner

import android.Manifest
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geg.fieldintel.camera.CameraXController
import com.geg.fieldintel.data.model.ScanResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

/**
 * "Camera scanner interface" — Teammate 2's AR Visualization requirement.
 * Shows a live CameraX preview with a viewfinder guide, captures a still frame on tap,
 * sends it to the AI Botanical Guide, and routes to the AR result overlay.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    onSpeciesIdentified: (ScanResult) -> Unit,
    onOpenChat: () -> Unit,
    viewModel: ScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val cameraController = remember { CameraXController(context) }

    LaunchedEffect(uiState) {
        if (uiState is ScannerUiState.Result) {
            onSpeciesIdentified((uiState as ScannerUiState.Result).result)
            viewModel.reset()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermission.status.isGranted) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        cameraController.startCamera(this, lifecycleOwner)
                    }
                }
            )

            // Viewfinder guide overlay
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(260.dp)
                    .background(Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, Color.White.copy(alpha = 0.7f), MaterialTheme.shapes.large)
                )
            }

            Text(
                text = "Center the plant, then tap to scan",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .background(Color.Black.copy(alpha = 0.4f), MaterialTheme.shapes.medium)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Capture button
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        val file = cameraController.capturePhoto()
                        viewModel.onPhotoCaptured(file)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .size(72.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Scan plant", tint = Color.White)
            }

            // Chat entry point
            FloatingActionButton(
                onClick = onOpenChat,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Filled.Chat, contentDescription = "Ask the AI Botanical Guide")
            }

            if (uiState is ScannerUiState.Scanning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text("Identifying species…", color = Color.White)
                    }
                }
            }
        } else {
            PermissionRequest(
                onRequest = { cameraPermission.launchPermissionRequest() }
            )
        }
    }
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Camera access is needed to scan native plants in the field.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("Grant camera permission") }
    }
}
