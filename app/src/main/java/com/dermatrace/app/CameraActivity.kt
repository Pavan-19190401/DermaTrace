package com.dermatrace.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.dermatrace.app.databinding.ActivityCameraBinding
import com.dermatrace.app.ml.ImageQualityAnalyzer
import com.dermatrace.app.ml.QualityLevel
import com.dermatrace.app.ml.QualityStatus
import com.dermatrace.app.util.HapticFeedbackHelper
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null
    private var isTorchOn = false
    private lateinit var cameraExecutor: ExecutorService

    private var currentQualityStatus: QualityStatus? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to capture lesions", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupGhostOverlay()
        setupListeners()

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupGhostOverlay() {
        val ghostPath = intent.getStringExtra("EXTRA_GHOST_PATH")
        if (!ghostPath.isNullOrEmpty()) {
            val bmp = BitmapFactory.decodeFile(ghostPath)
            if (bmp != null) {
                binding.ivGhostOverlay.setImageBitmap(bmp)
                binding.ivGhostOverlay.visibility = View.VISIBLE
                binding.ghostControlDock.visibility = View.VISIBLE
                binding.ivGhostOverlay.alpha = 0.45f
                binding.seekGhostAlpha.progress = 45
            }
        }

        binding.seekGhostAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val clamped = progress.coerceIn(5, 95)
                val alphaVal = clamped / 100f
                binding.ivGhostOverlay.alpha = alphaVal
                binding.tvGhostAlphaValue.text = "${clamped}%"
                if (fromUser && clamped % 10 == 0) {
                    HapticFeedbackHelper.performTick(seekBar)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.tvGhostLabel.setOnClickListener {
            val isVis = binding.ivGhostOverlay.visibility == View.VISIBLE
            binding.ivGhostOverlay.visibility = if (isVis) View.GONE else View.VISIBLE
            binding.tvGhostLabel.alpha = if (isVis) 0.5f else 1.0f
            HapticFeedbackHelper.performTick(it)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            HapticFeedbackHelper.performTick(it)
            finish()
        }

        binding.btnCapture.setOnClickListener {
            takePhoto()
        }

        binding.btnToggleTorch.setOnClickListener {
            toggleTorch()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Real-Time Edge AI Quality Gate Analyzer running at 30 FPS
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor, ImageQualityAnalyzer { status ->
                        runOnUiThread {
                            updateQualityUI(status)
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to bind camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("SetTextI18n")
    private fun updateQualityUI(status: QualityStatus) {
        currentQualityStatus = status
        binding.tvGuidanceTitle.text = status.guidanceMessage
        binding.tvQualityMetrics.text = String.format(
            "Sharpness (Laplacian): %.1f | Light: %.0f",
            status.sharpnessScore,
            status.brightnessScore
        )

        when (status.level) {
            QualityLevel.OPTIMAL -> {
                binding.statusDot.setBackgroundResource(R.drawable.bg_status_dot_ready)
                binding.reticleRing.setBackgroundResource(R.drawable.bg_reticle_ready)
                binding.tvShutterHint.text = "✓ Optimal Focus • Tap to Capture"
                binding.tvShutterHint.setTextColor(0xFF34D399.toInt())
            }
            QualityLevel.BLURRY -> {
                binding.statusDot.setBackgroundResource(R.drawable.bg_status_dot_error)
                binding.reticleRing.setBackgroundResource(R.drawable.bg_reticle_warning)
                binding.tvShutterHint.text = "Hold Steady • Focusing..."
                binding.tvShutterHint.setTextColor(0xFFF87171.toInt())
            }
            QualityLevel.TOO_DARK, QualityLevel.GLARE -> {
                binding.statusDot.setBackgroundResource(R.drawable.bg_status_dot_warning)
                binding.reticleRing.setBackgroundResource(R.drawable.bg_reticle_warning)
                binding.tvShutterHint.text = "Adjust Lighting / Torch"
                binding.tvShutterHint.setTextColor(0xFFFBBF24.toInt())
            }
        }
    }

    private fun toggleTorch() {
        val cam = camera ?: return
        if (cam.cameraInfo.hasFlashUnit()) {
            isTorchOn = !isTorchOn
            cam.cameraControl.enableTorch(isTorchOn)
            HapticFeedbackHelper.performTick(binding.btnToggleTorch)
            Toast.makeText(this, if (isTorchOn) "Torch ON" else "Torch OFF", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Torch not available on device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val status = currentQualityStatus

        // Warn if motion blur is active
        if (status != null && status.level == QualityLevel.BLURRY) {
            HapticFeedbackHelper.performWarning(this)
            Toast.makeText(this, "Warning: Image may be blurry. Hold steady.", Toast.LENGTH_SHORT).show()
        }

        HapticFeedbackHelper.performShutter(this)
        val photoFile = File(externalCacheDir, "lesion_scan_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        binding.btnCapture.isEnabled = false

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    binding.btnCapture.isEnabled = true
                    Toast.makeText(baseContext, "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    binding.btnCapture.isEnabled = true
                    HapticFeedbackHelper.performSuccess(baseContext)
                    val resultIntent = intent
                    resultIntent.putExtra("CAPTURED_IMAGE_PATH", photoFile.absolutePath)
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
