package com.example.crosswordsai.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

// Utility function to get an Executor for the camera
private val Context.executor: Executor
    get() = ContextCompat.getMainExecutor(this)

// Function to take a photo
private fun takePhoto(
    filenameFormat: String,
    imageCapture: ImageCapture,
    outputDirectory: File,
    executor: Executor,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val photoFile = File(
        outputDirectory,
        SimpleDateFormat(filenameFormat, Locale.US).format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onError(exception: ImageCaptureException) {
            Log.e("CameraCaptureView", "Error capturing image: ${exception.message}", exception)
            onError(exception)
        }

        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            val savedUri = Uri.fromFile(photoFile)
            onImageCaptured(savedUri)
        }
    })
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun CameraCaptureView(
    filenameFormat: String = "yyyy-MM-dd-HH-mm-ss-SSS",
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    onImageCaptured: (Uri) -> Unit, // callback when a picture is taken
    onError: (ImageCaptureException) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera preview configuration
    val preview = Preview.Builder().build()
    val cameraSelector = remember {
        CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK) // Use the back camera
            .build()
    }

    // Image capture configuration
    val imageCapture = remember {
        ImageCapture.Builder()
            .setTargetRotation(context.display.rotation)
            .build()
    }

    // Output directory for photos
    val outputDirectory = remember { context.filesDir } // Or a shared folder if it needs to be accessible in the gallery

    // The AndroidView is now the only place that manages the preview binding.
    // The previous LaunchedEffect is no longer necessary in this form.
    // We just need to make sure that cameraProvider is bound only once
    // and that the surfaceProvider is set.

    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview view
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                // Important: Bind CameraX use cases here in the factory,
                // or ensure it's done only once and correctly handled across recompositions.
                // The LaunchedEffect inside AndroidView's factory ensures this happens once
                // when the view is created.
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    preview.surfaceProvider = previewView.surfaceProvider
                    try {
                        // Unbind all, then rebind the camera use cases
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                    } catch (exc: Exception) {
                        Log.e("CameraCaptureView", "Use case binding failed: ${exc.message}", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Capture button at the bottom of the screen
        FloatingActionButton(
            onClick = {
                takePhoto(
                    filenameFormat = filenameFormat,
                    imageCapture = imageCapture,
                    outputDirectory = outputDirectory,
                    executor = context.executor,
                    onImageCaptured = onImageCaptured,
                    onError = onError
                )
            },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(72.dp) // Large size for the button
        ) {
            Icon(
                imageVector = Icons.Filled.Camera,
                contentDescription = "Take a picture",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}