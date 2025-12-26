package com.bluemix.clients_lead.features.Clients.presentation

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/* ========================================================= */
/* ================= IMAGE CAPTURE DIALOG ================== */
/* ========================================================= */

@Composable
fun ImageCaptureDialog(
    onImageCaptured: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
    ) {
        when {
            !hasPermission -> {
                PermissionDeniedScreen(
                    onDismiss = onDismiss,
                    onRequestAgain = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }

            capturedBitmap != null -> {
                ImagePreviewScreen(
                    bitmap = capturedBitmap!!,
                    onConfirm = { onImageCaptured(capturedBitmap!!) },
                    onRetake = { capturedBitmap = null },
                    onDismiss = onDismiss
                )
            }

            else -> {
                CameraScreen(
                    context = context,
                    onImageCaptured = { bitmap ->
                        capturedBitmap = bitmap
                    },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

/* ========================================================= */
/* ===================== CAMERA SCREEN ===================== */
/* ========================================================= */

@Composable
private fun CameraScreen(
    context: Context,
    onImageCaptured: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    DisposableEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            providerFuture.get().unbindAll()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable {
                    scope.launch {
                        imageCapture?.let {
                            takePicture(context, it)?.let(onImageCaptured)
                        }
                    }
                }
        )
    }
}

/* ========================================================= */
/* ==================== IMAGE PREVIEW ====================== */
/* ========================================================= */

@Composable
private fun ImagePreviewScreen(
    bitmap: Bitmap,
    onConfirm: () -> Unit,
    onRetake: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onRetake) { Text("Retake") }
            Button(onClick = onConfirm) { Text("Use Image") }
        }
    }
}

/* ========================================================= */
/* ================= PERMISSION SCREEN ===================== */
/* ========================================================= */

@Composable
private fun PermissionDeniedScreen(
    onDismiss: () -> Unit,
    onRequestAgain: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Camera permission required",
                color = Color.White,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRequestAgain) {
                Text("Grant Permission")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    }
}

/* ========================================================= */
/* ===================== HELPERS =========================== */
/* ========================================================= */

private suspend fun takePicture(
    context: Context,
    imageCapture: ImageCapture
): Bitmap? = suspendCoroutine { cont ->
    val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")

    val options = ImageCapture.OutputFileOptions.Builder(file).build()

    imageCapture.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    cont.resume(rotateBitmap(bitmap, 90f))
                } catch (e: Exception) {
                    Timber.e(e)
                    cont.resume(null)
                } finally {
                    file.delete()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Timber.e(exception)
                cont.resume(null)
                file.delete()
            }
        }
    )
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
