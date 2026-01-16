package com.bluemix.clients_lead.features.Clients.presentation

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.compose.foundation.BorderStroke
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
    var captureMode by remember { mutableStateOf<CaptureMode>(CaptureMode.CAMERA) }
    var showAccuracyIndicator by remember { mutableStateOf(false) }
    var imageQuality by remember { mutableStateOf(ImageQuality.UNKNOWN) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // ✅ Use safe image decoding with downsampling
                val bitmap = decodeSampledBitmapFromUri(
                    context = context,
                    uri = uri,
                    reqWidth = 1920,
                    reqHeight = 1080
                )
                bitmap?.let { bmp ->
                    capturedBitmap = bmp
                    imageQuality = analyzeImageQuality(bmp)
                    showAccuracyIndicator = true
                }
            } catch (e: OutOfMemoryError) {
                Timber.e(e, "Out of memory loading gallery image")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load image from gallery")
            }
        }
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
                    imageQuality = imageQuality,
                    showQualityIndicator = showAccuracyIndicator,
                    onConfirm = { onImageCaptured(capturedBitmap!!) },
                    onRetake = {
                        capturedBitmap = null
                        showAccuracyIndicator = false
                    },
                    onDismiss = onDismiss
                )
            }

            else -> {
                CameraScreen(
                    context = context,
                    onImageCaptured = { bitmap, quality ->
                        capturedBitmap = bitmap
                        imageQuality = quality
                        showAccuracyIndicator = true
                    },
                    onDismiss = onDismiss,
                    onSwitchToGallery = {
                        captureMode = CaptureMode.GALLERY
                        galleryLauncher.launch("image/*")
                    }
                )
            }
        }
    }
}

/* ========================================================= */
/* ==================== DATA MODELS ======================== */
/* ========================================================= */

enum class CaptureMode {
    CAMERA, GALLERY
}

enum class ImageQuality {
    EXCELLENT,  // Sharp, well-lit, high resolution
    GOOD,       // Acceptable quality
    FAIR,       // Slightly blurry or low light
    POOR,       // Very blurry, dark, or low resolution
    UNKNOWN
}

/* ========================================================= */
/* ===================== CAMERA SCREEN ===================== */
/* ========================================================= */

@Composable
private fun CameraScreen(
    context: Context,
    onImageCaptured: (Bitmap, ImageQuality) -> Unit,
    onDismiss: () -> Unit,
    onSwitchToGallery: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

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

        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }

            // Gallery button
            OutlinedButton(
                onClick = onSwitchToGallery,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White)
            ) {
                Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gallery")
            }
        }

        // Capture hint
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "📸 Position card in frame",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        // Capture button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(60.dp),
                    color = Color.White,
                    strokeWidth = 4.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            scope.launch {
                                isCapturing = true
                                imageCapture?.let {
                                    val result = takePicture(context, it)
                                    result?.let { (bitmap, quality) ->
                                        onImageCaptured(bitmap, quality)
                                    }
                                }
                                isCapturing = false
                            }
                        }
                )
            }

            Text(
                text = "Tap to capture",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

/* ========================================================= */
/* ==================== IMAGE PREVIEW ====================== */
/* ========================================================= */

@Composable
private fun ImagePreviewScreen(
    bitmap: Bitmap,
    imageQuality: ImageQuality,
    showQualityIndicator: Boolean,
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

        // Quality indicator overlay
        AnimatedVisibility(
            visible = showQualityIndicator,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            QualityIndicatorCard(imageQuality)
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Retake")
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5E92F3)
                )
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Use Image")
            }
        }
    }
}

@Composable
private fun QualityIndicatorCard(quality: ImageQuality) {
    val (bgColor, icon, text, textColor) = when (quality) {
        ImageQuality.EXCELLENT -> listOf(
            Color(0xFF4CAF50).copy(alpha = 0.9f),
            "✓",
            "Excellent quality",
            Color.White
        )
        ImageQuality.GOOD -> listOf(
            Color(0xFF8BC34A).copy(alpha = 0.9f),
            "✓",
            "Good quality",
            Color.White
        )
        ImageQuality.FAIR -> listOf(
            Color(0xFFFFC107).copy(alpha = 0.9f),
            "⚠",
            "Fair quality - Consider retaking",
            Color.Black
        )
        ImageQuality.POOR -> listOf(
            Color(0xFFFF5252).copy(alpha = 0.9f),
            "✗",
            "Poor quality - Please retake",
            Color.White
        )
        ImageQuality.UNKNOWN -> listOf(
            Color(0xFF757575).copy(alpha = 0.9f),
            "?",
            "Unknown quality",
            Color.White
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor as Color),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon as String,
                fontSize = 20.sp,
                color = textColor as Color
            )
            Text(
                text = text as String,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.7f)
            )

            Text(
                text = "Camera permission required",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Allow camera access to scan business cards",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onRequestAgain,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5E92F3)
                )
            ) {
                Text("Grant Permission")
            }

            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    }
}

/* ========================================================= */
/* ===================== HELPERS =========================== */
/* ========================================================= */



/**
 * Decode bitmap with downsampling to avoid OOM on old devices
 */
private fun decodeSampledBitmapFromUri(
    context: Context,
    uri: Uri,
    reqWidth: Int,
    reqHeight: Int
): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(stream, null, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory

            context.contentResolver.openInputStream(uri)?.use { stream2 ->
                BitmapFactory.decodeStream(stream2, null, options)
            }
        }
    } catch (e: Exception) {
        Timber.e(e, "Error decoding bitmap")
        null
    }
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}
private suspend fun takePicture(
    context: Context,
    imageCapture: ImageCapture
): Pair<Bitmap, ImageQuality>? = suspendCoroutine { cont ->
    val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")

    val options = ImageCapture.OutputFileOptions.Builder(file).build()

    imageCapture.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    val rotatedBitmap = rotateBitmap(bitmap, 90f)
                    val quality = analyzeImageQuality(rotatedBitmap)
                    cont.resume(Pair(rotatedBitmap, quality))
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

/**
 * Analyze image quality based on multiple factors
 */
private fun analyzeImageQuality(bitmap: Bitmap): ImageQuality {
    try {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = width * height

        // Resolution check
        val resolutionScore = when {
            pixels >= 2_000_000 -> 3  // Excellent (2MP+)
            pixels >= 1_000_000 -> 2  // Good (1MP+)
            pixels >= 500_000 -> 1    // Fair (0.5MP+)
            else -> 0                  // Poor
        }

        // Brightness check (sample center pixels)
        val centerX = width / 2
        val centerY = height / 2
        val sampleSize = minOf(100, width / 4, height / 4)

        var totalBrightness = 0
        var sampleCount = 0

        for (x in (centerX - sampleSize)..(centerX + sampleSize) step 10) {
            for (y in (centerY - sampleSize)..(centerY + sampleSize) step 10) {
                if (x in 0 until width && y in 0 until height) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    totalBrightness += (r + g + b) / 3
                    sampleCount++
                }
            }
        }

        val avgBrightness = if (sampleCount > 0) totalBrightness / sampleCount else 128

        val brightnessScore = when {
            avgBrightness in 100..200 -> 2  // Good lighting
            avgBrightness in 80..220 -> 1   // Acceptable
            else -> 0                        // Too dark or overexposed
        }

        // Combined score
        val totalScore = resolutionScore + brightnessScore

        return when {
            totalScore >= 4 -> ImageQuality.EXCELLENT
            totalScore >= 3 -> ImageQuality.GOOD
            totalScore >= 2 -> ImageQuality.FAIR
            else -> ImageQuality.POOR
        }

    } catch (e: Exception) {
        Timber.e(e, "Failed to analyze image quality")
        return ImageQuality.UNKNOWN
    }
}