package com.bluemix.clients_lead.core.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageCompressionUtils {

    /**
     * Compress image to WebP format
     * @param context Android context
     * @param uri Source image URI
     * @param maxWidth Maximum width (default 1280px)
     * @param maxHeight Maximum height (default 1280px)
     * @param quality WebP quality 0-100 (default 80)
     * @return Base64 encoded WebP string
     */
    suspend fun compressToWebP(
        context: Context,
        uri: Uri,
        maxWidth: Int = 800,
        maxHeight: Int = 600,
        quality: Int = 60
    ): Result<String> {
        return try {
            Timber.d("📸 Starting WebP compression for: $uri")

            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                return Result.failure(Exception("Failed to decode image"))
            }

            val rotatedBitmap = fixImageRotation(context, uri, originalBitmap)
            val resizedBitmap = resizeBitmap(rotatedBitmap, maxWidth, maxHeight)

            val outputStream = ByteArrayOutputStream()
            val compressed = resizedBitmap.compress(
                Bitmap.CompressFormat.WEBP_LOSSY,
                quality,
                outputStream
            )

            if (!compressed) {
                return Result.failure(Exception("WebP compression failed"))
            }

            val webpBytes = outputStream.toByteArray()
            val base64String = android.util.Base64.encodeToString(
                webpBytes,
                android.util.Base64.NO_WRAP
            )

            outputStream.close()

            if (rotatedBitmap != originalBitmap) rotatedBitmap.recycle()
            if (resizedBitmap != rotatedBitmap) resizedBitmap.recycle()
            originalBitmap.recycle()

            Result.success(base64String)
        } catch (e: Exception) {
            Timber.e(e, "❌ Image compression failed")
            Result.failure(e)
        }
    }

    /**
     * Compress image and save as WebP file
     * Returns file URI string
     */
    suspend fun compressToWebPFile(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1280,
        maxHeight: Int = 1280,
        quality: Int = 80
    ): Result<String> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                return Result.failure(Exception("Failed to decode image"))
            }

            val rotatedBitmap = fixImageRotation(context, uri, originalBitmap)
            val resizedBitmap = resizeBitmap(rotatedBitmap, maxWidth, maxHeight)

            val outputFile = File(
                context.cacheDir,
                "receipt_${System.currentTimeMillis()}.webp"
            )

            FileOutputStream(outputFile).use { fos ->
                resizedBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, fos)
            }

            if (rotatedBitmap != originalBitmap) rotatedBitmap.recycle()
            if (resizedBitmap != rotatedBitmap) resizedBitmap.recycle()
            originalBitmap.recycle()

            Result.success(outputFile.absolutePath)
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to save WebP file")
            Result.failure(e)
        }
    }


    /**
     * Fix image rotation based on EXIF orientation
     */
    private fun fixImageRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = ExifInterface(inputStream!!)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Timber.w(e, "⚠️ Could not fix rotation, using original")
            bitmap
        }
    }

    /**
     * Resize bitmap while maintaining aspect ratio
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val aspectRatio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxWidth
            newHeight = (maxWidth / aspectRatio).toInt()
        } else {
            newHeight = maxHeight
            newWidth = (maxHeight * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Get file size in human-readable format
     */
    fun getReadableFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}