package com.agroroute.features.farmer.listing

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.agroroute.core.theme.AgroRouteTheme
import com.github.thelacspace.icons.Lucide
import java.io.OutputStream

/** Private capture/review surface. Returns only acceptance; preparation and uploads stay in the form. */
class ListingCameraActivity : ComponentActivity() {
    companion object { const val CAPTURE_NAME = "listing_capture_name" }
    private var captureName: String? = null
    private var accepted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = intent.getStringExtra(CAPTURE_NAME)
        if (name == null || runCatching { ListingCameraPhoto.uri(this, name) }.isFailure) {
            finish(); return
        }
        captureName = name
        setContent {
            AgroRouteTheme(darkTheme = true) {
                HarvestCamera(name, onCancel = { finish() }, onUse = {
                    accepted = true
                    setResult(RESULT_OK)
                    finish()
                })
            }
        }
    }

    override fun onDestroy() {
        // Never remove accepted files: the listing form owns their lifetime from this point.
        if (isFinishing && !accepted) captureName?.let { ListingCameraPhoto.discard(this, it) }
        super.onDestroy()
    }

    @Composable
    private fun HarvestCamera(name: String, onCancel: () -> Unit, onUse: () -> Unit) {
        var reviewing by rememberSaveable { mutableStateOf(false) }
        var capturing by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
        val lifecycleOwner = LocalLifecycleOwner.current
        val previewView = remember { PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        } }
        val executor = remember { ContextCompat.getMainExecutor(this) }
        val uri = remember(name) { ListingCameraPhoto.uri(this, name) }
        BackHandler { onCancel() }

        if (!reviewing) DisposableEffect(lifecycleOwner, previewView) {
            var disposed = false
            var provider: ProcessCameraProvider? = null
            val preview = Preview.Builder().build()
            val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
            val future = ProcessCameraProvider.getInstance(this@ListingCameraActivity)
            future.addListener({
                if (!disposed) {
                    try {
                        check(ContextCompat.checkSelfPermission(this@ListingCameraActivity, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED)
                        val current = future.get()
                        provider = current
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                        current.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                        imageCapture = capture
                        error = null
                    } catch (_: Exception) {
                        error = "Couldn't start the camera. Close this screen and try again, or choose from Gallery."
                    }
                }
            }, executor)
            onDispose {
                disposed = true
                imageCapture = null
                provider?.unbind(preview, capture)
            }
        }

        fun capturePhoto() {
            val capture = imageCapture ?: return
            if (capturing || reviewing) return
            capturing = true
            error = null
            var stream: OutputStream? = null
            try {
                stream = contentResolver.openOutputStream(uri, "w") ?: throw IllegalStateException("Capture storage unavailable")
                val output = requireNotNull(stream)
                capture.targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
                capture.takePicture(ImageCapture.OutputFileOptions.Builder(output).build(), executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                            runCatching { output.close() }
                            capturing = false
                            if (!isFinishing && !isDestroyed) reviewing = true
                            else if (!accepted) ListingCameraPhoto.discard(this@ListingCameraActivity, name)
                        }
                        override fun onError(exception: ImageCaptureException) {
                            runCatching { output.close() }
                            capturing = false
                            error = "Couldn't capture the photo. Please try again."
                            if (isFinishing || isDestroyed) ListingCameraPhoto.discard(this@ListingCameraActivity, name)
                        }
                    })
            } catch (_: Exception) {
                runCatching { stream?.close() }
                capturing = false
                error = "Couldn't save the photo. Please try again or choose from Gallery."
            }
        }

        Surface(Modifier.fillMaxSize(), color = Color(0xFF071B13)) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCancel) { Icon(Lucide.X, "Close camera") }
                    Column {
                        Text("Take Harvest Photo", style = MaterialTheme.typography.titleLarge)
                        Text(if (reviewing) "Check your harvest photo" else "Rear camera · Keep your harvest in focus",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black), contentAlignment = Alignment.Center) {
                    if (reviewing) AsyncImage(coil.request.ImageRequest.Builder(this@ListingCameraActivity)
                        .data(uri).memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                        .diskCachePolicy(coil.request.CachePolicy.DISABLED).build(),
                        "Captured harvest photo", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    else AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize().testTag("harvest-camera-preview"))
                    if (capturing || (imageCapture == null && !reviewing && error == null)) CircularProgressIndicator()
                }
                error?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    if (reviewing) {
                        OutlinedButton(onClick = {
                            ListingCameraPhoto.discard(this@ListingCameraActivity, name)
                            reviewing = false
                            error = null
                        }, modifier = Modifier.weight(1f)) { Text("Retake") }
                        Spacer(Modifier.width(16.dp))
                        Button(onClick = onUse, modifier = Modifier.weight(1f)) { Text("Use Photo") }
                    } else {
                        FilledIconButton(onClick = ::capturePhoto, enabled = imageCapture != null && !capturing,
                            modifier = Modifier.size(76.dp).border(2.dp, Color.White, CircleShape)) {
                            Icon(Lucide.Camera, "Capture photo", Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}
