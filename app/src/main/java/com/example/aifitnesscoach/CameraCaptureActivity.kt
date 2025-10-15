package com.example.aifitnesscoach

import android.content.Intent
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.aifitnesscoach.databinding.ActivityCameraCaptureBinding
import com.example.aifitnesscoach.network.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraCaptureBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isFrontalImage = true // To track which image we are capturing

    private var frontalImageUri: Uri? = null
    private var sideImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        binding.captureButton.setOnClickListener {
            takePhoto()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    if (isFrontalImage) {
                        frontalImageUri = savedUri
                        isFrontalImage = false
                        binding.instructionTextView.text = "Now, please take a side-view photo."
                        // You can also show the captured image preview here if you want
                    } else {
                        sideImageUri = savedUri
                        // Now we have both images, let's upload them
                        uploadImages()
                    }
                }
            })
    }

    private fun uploadImages() {
        if (frontalImageUri == null || sideImageUri == null) {
            Toast.makeText(this, "Please capture both images.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val frontalFile = File(frontalImageUri!!.path!!)
                val sideFile = File(sideImageUri!!.path!!)

                val frontalRequestBody = frontalFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val sideRequestBody = sideFile.asRequestBody("image/jpeg".toMediaTypeOrNull())

                val frontalPart = MultipartBody.Part.createFormData("frontal_image", frontalFile.name, frontalRequestBody)
                val sidePart = MultipartBody.Part.createFormData("side_image", sideFile.name, sideRequestBody)

                val response = RetrofitClient.instance.predictBiometrics(frontalPart, sidePart)

                // On success, navigate to the OnboardingFormActivity
                val intent = Intent(this@CameraCaptureActivity, OnboardingFormActivity::class.java).apply {
                    // Pass the biometrics data as a JSON string
                    putExtra("BIOMETRICS_DATA", Gson().toJson(response.biometrics))

                    // **ADD THESE TWO LINES**
                    putExtra("FRONTAL_IMAGE_URI", frontalImageUri.toString())
                    putExtra("SIDE_IMAGE_URI", sideImageUri.toString())
                }
                startActivity(intent)
                finish() // Finish this activity

            } catch (e: Exception) {
                Log.e(TAG, "Error uploading images", e)
                Toast.makeText(this@CameraCaptureActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                // Reset for re-capture
                isFrontalImage = true
                frontalImageUri = null
                sideImageUri = null
                binding.instructionTextView.text = "Please take a frontal-view photo."
            }
        }
    }

    private val outputDirectory: File by lazy {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        mediaDir ?: filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraCaptureActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}