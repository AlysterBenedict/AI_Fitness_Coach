package com.example.aifitnesscoach

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.aifitnesscoach.databinding.ActivityCameraCaptureBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraCaptureBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    private var captureState = CaptureState.FRONTAL
    private var frontalImageUri: Uri? = null
    private var sideImageUri: Uri? = null

    private enum class CaptureState {
        FRONTAL, SIDE
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val TAG = "CameraCaptureActivity"
        const val EXTRA_FRONTAL_IMAGE_URI = "EXTRA_FRONTAL_IMAGE_URI"
        const val EXTRA_SIDE_IMAGE_URI = "EXTRA_SIDE_IMAGE_URI"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.captureButton.setOnClickListener { takePhoto() }
        updateUIForState()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(this, "Failed to start camera.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            "${System.currentTimeMillis()}_${captureState.name.lowercase()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    handleImageSaved(savedUri)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(baseContext, "Photo capture failed.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun handleImageSaved(savedUri: Uri) {
        when (captureState) {
            CaptureState.FRONTAL -> {
                frontalImageUri = savedUri
                Toast.makeText(this, "Frontal image captured!", Toast.LENGTH_SHORT).show()
                captureState = CaptureState.SIDE
                updateUIForState()
            }
            CaptureState.SIDE -> {
                sideImageUri = savedUri
                Toast.makeText(this, "Side image captured!", Toast.LENGTH_SHORT).show()
                proceedToNextStep()
            }
        }
    }

    private fun updateUIForState() {
        when (captureState) {
            CaptureState.FRONTAL -> {
                binding.instructionText.text = "Align Your Body: Frontal View"
                binding.outlineOverlay.setImageResource(R.drawable.ic_outline_frontal)
            }
            CaptureState.SIDE -> {
                binding.instructionText.text = "Align Your Body: Side View"
                binding.outlineOverlay.setImageResource(R.drawable.ic_outline_side)
            }
        }
    }

    // --- THIS FUNCTION IS NOW CORRECTED ---
    private fun proceedToNextStep() {
        val intent = Intent(this, OnboardingFormActivity::class.java).apply {
            putExtra(EXTRA_FRONTAL_IMAGE_URI, frontalImageUri.toString())
            putExtra(EXTRA_SIDE_IMAGE_URI, sideImageUri.toString())
        }
        startActivity(intent)
        finish() // Close camera activity
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}