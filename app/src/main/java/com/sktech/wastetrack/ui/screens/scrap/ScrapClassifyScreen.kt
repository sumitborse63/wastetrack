package com.sktech.wastetrack.ui.screens.scrap

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.R
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.ui.theme.*
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrapClassifyScreen(
    onNavigateBack: () -> Unit,
    onClassificationComplete: (ScrapCategory, String) -> Unit,
    viewModel: ScrapClassifyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    var isFlashEnabled by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Bind Flash / Torch setting when state or camera changes
    LaunchedEffect(isFlashEnabled, camera, imageCapture) {
        imageCapture?.flashMode = if (isFlashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        camera?.cameraControl?.enableTorch(isFlashEnabled)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.capturing_ai),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Edge-AI Scrap Classifier & Density Inspector",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (state.capturedImage == null && hasCameraPermission) {
                        IconButton(onClick = { isFlashEnabled = !isFlashEnabled }) {
                            Icon(
                                imageVector = if (isFlashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                contentDescription = if (isFlashEnabled) "Flash On" else "Flash Off",
                                tint = if (isFlashEnabled) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!hasCameraPermission) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.padding(24.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(56.dp), tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(stringResource(R.string.camera_permission_required), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text(stringResource(R.string.grant_permission), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (state.capturedImage == null) {
                // Camera Preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setFlashMode(if (isFlashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                                .build()

                            imageCapture = capture

                            try {
                                cameraProvider.unbindAll()
                                val boundCamera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture
                                )
                                camera = boundCamera
                                boundCamera.cameraControl.enableTorch(isFlashEnabled)
                            } catch (e: Exception) {
                                // Error handling
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    IconButton(
                        onClick = {
                            val executor = Executors.newSingleThreadExecutor()
                            imageCapture?.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val buffer = image.planes[0].buffer
                                    val bytes = ByteArray(buffer.remaining())
                                    buffer.get(bytes)
                                    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

                                    val rotation = image.imageInfo.rotationDegrees
                                    if (rotation != 0) {
                                        val matrix = Matrix()
                                        matrix.postRotate(rotation.toFloat())
                                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                    }

                                    viewModel.analyzeImage(bitmap, 0)
                                    image.close()
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    // Error handling
                                }
                            })
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Capture", modifier = Modifier.size(36.dp), tint = Color.White)
                    }
                }
            } else {
                // Analysis Result
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Image(
                            bitmap = state.capturedImage!!.asImageBitmap(),
                            contentDescription = "Captured Scrap",
                            modifier = Modifier.fillMaxSize()
                        )

                        if (state.isAnalyzing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = EmeraldPrimary)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(stringResource(R.string.analyzing_with_ai), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (state.result != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shadowElevation = 4.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    stringResource(R.string.select_category),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${state.result!!.category.icon} ${stringResource(state.result!!.category.nameRes)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = state.result!!.category.color()
                                )

                                if (state.result!!.subCategory.isNotBlank()) {
                                    Surface(
                                        color = EmeraldContainer,
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.padding(top = 6.dp)
                                    ) {
                                        Text(
                                            text = state.result!!.subCategory,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (state.result!!.confidence >= 0.90f) EmeraldContainer else TealContainer,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = "Confidence: ${(state.result!!.confidence * 100).toInt()}%",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (state.result!!.confidence >= 0.90f) EmeraldPrimary else Teal
                                        )
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = state.result!!.engine,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.reset() },
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text(stringResource(R.string.retake), fontWeight = FontWeight.SemiBold)
                                    }
                                    Button(
                                        onClick = { onClassificationComplete(state.result!!.category, state.result!!.subCategory) },
                                        shape = MaterialTheme.shapes.medium,
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text(stringResource(R.string.use_result), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else if (state.error != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = AlertRedContainer,
                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(state.error!!, color = AlertRed, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.reset() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                                ) {
                                    Text(stringResource(R.string.try_again), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
