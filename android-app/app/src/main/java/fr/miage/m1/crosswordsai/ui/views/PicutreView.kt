package fr.miage.m1.crosswordsai.ui.views

import android.Manifest
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import fr.miage.m1.crosswordsai.viewmodel.CrosswordViewModel
import fr.miage.m1.crosswordsai.viewmodel.ProcessingState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PictureView(
    modifier: Modifier = Modifier,
    viewModel: CrosswordViewModel = viewModel(),
    onNavigateToGrid: () -> Unit = {}
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    val processingState by viewModel.processingState.collectAsState()

    // Configurer le callback de navigation
    viewModel.setOnGridReadyCallback(onNavigateToGrid)

    // Launcher pour la galerie
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            capturedImageUri = it
            Log.d("PictureView", "Gallery Image: $uri")
        }
    }

    when {
        cameraPermissionState.status.isGranted -> {
            // Permission accordée - Afficher la caméra ou l'image capturée
            when {
                // Affichage de l'état de traitement
                processingState !is ProcessingState.Idle && processingState !is ProcessingState.Complete -> {
                    ProcessingStateView(
                        state = processingState,
                        modifier = modifier,
                        onCancel = {
                            capturedImageUri = null
                            viewModel.reset()
                        }
                    )
                }
                // Image capturée - Afficher la prévisualisation
                capturedImageUri != null -> {
                    ImagePreviewView(
                        imageUri = capturedImageUri!!,
                        modifier = modifier,
                        onRetake = { capturedImageUri = null },
                        onValidate = {
                            viewModel.processImage(capturedImageUri!!)
                        },
                        onPickFromGallery = { galleryLauncher.launch("image/*") }
                    )
                }
                // Pas d'image - Afficher la caméra
                else -> {
                    Box(modifier = modifier.fillMaxSize()) {
                        CameraCaptureView(
                            modifier = Modifier.fillMaxSize(),
                            onImageCaptured = { uri ->
                                capturedImageUri = uri
                                Log.d("PictureView", "Captured Image: $uri")
                            },
                            onError = { exception ->
                                Log.e("PictureView", "Error while capturing image: ${exception.message}")
                            }
                        )

                        // Bouton galerie en bas à gauche
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 32.dp, bottom = 40.dp)
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Galerie",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Galerie")
                        }
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
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Image, "Galerie")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choisir depuis la galerie")
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
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Image, "Galerie")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choisir depuis la galerie")
                }
            }
        }
    }
}

/**
 * Vue de prévisualisation de l'image avec boutons de validation
 */
@Composable
private fun ImagePreviewView(
    imageUri: Uri,
    modifier: Modifier = Modifier,
    onRetake: () -> Unit,
    onValidate: () -> Unit,
    onPickFromGallery: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Prévisualisation",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Image(
            painter = rememberAsyncImagePainter(model = imageUri),
            contentDescription = "Image capturée",
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .weight(1f)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Boutons d'action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Reprendre
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Close, "Reprendre")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reprendre")
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Valider
            Button(
                onClick = onValidate,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Check, "Valider")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Valider")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bouton galerie
        OutlinedButton(onClick = onPickFromGallery) {
            Icon(Icons.Default.Image, "Galerie")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choisir une autre image")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Vue affichant l'état du traitement en cours
 */
@Composable
private fun ProcessingStateView(
    state: ProcessingState,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (state) {
            is ProcessingState.Preprocessing -> {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Prétraitement de l'image...",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            is ProcessingState.Analyzing -> {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Analyse de la grille...",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            is ProcessingState.Solving -> {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Résolution en cours...",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Les mots apparaissent dans la grille",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is ProcessingState.Error -> {
                Text(
                    "❌ Erreur",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onCancel) {
                    Text("Réessayer")
                }
            }
            else -> {}
        }
    }
}
