package com.example.crosswordsai.ui.views

import android.Manifest
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PictureView(modifier: Modifier = Modifier) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    when {
        cameraPermissionState.status.isGranted -> {
            // Permission accordée - Afficher la caméra ou l'image capturée
            if (capturedImageUri == null) {
                CameraCaptureView(
                    modifier = modifier,
                    onImageCaptured = { uri ->
                        capturedImageUri = uri
                        Log.d("PictureView", "Captured Image: $uri")
                    },
                    onError = { exception ->
                        Log.e("PictureView", "Error while capturing image: ${exception.message}")
                    }
                )
            } else {
                // Afficher l'image capturée
                Column(
                    modifier = modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = capturedImageUri),
                        contentDescription = "Image capturée",
                        modifier = Modifier
                            .fillMaxSize(0.8f)
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { capturedImageUri = null }) {
                        Text("Reprendre une photo")
                    }
                }
            }
        }
        cameraPermissionState.status.shouldShowRationale -> {
            // L'utilisateur a refusé une fois, montrer une explication
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "L'accès à la caméra est nécessaire pour prendre des photos.",
                    modifier = Modifier.padding(16.dp)
                )
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Demander la permission de la caméra")
                }
            }
        }
        else -> {
            // Permission non demandée ou refusée définitivement
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Veuillez autoriser l'accès à la caméra dans les paramètres de l'application",
                    modifier = Modifier.padding(16.dp)
                )
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Demander la permission")
                }
            }
        }
    }
}