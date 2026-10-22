package com.dermatrace.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dermatrace.app.data.db.DermaDatabase
import com.dermatrace.app.databinding.ActivityMainBinding
import com.dermatrace.app.ml.TfliteLesionAnalyzer
import com.dermatrace.app.ui.DermaTraceBridge
import com.dermatrace.app.util.BiometricSecurityManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bridge: DermaTraceBridge
    private lateinit var analyzer: TfliteLesionAnalyzer
    private lateinit var db: DermaDatabase

    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var isAuthenticating = false
    private var isInternalLaunch = false
    private var isSessionAuthenticated = false

    // Dedicated CameraX Activity launcher with Ghosting Alignment & Real-Time Quality Gate
    private val cameraScanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val path = result.data?.getStringExtra("CAPTURED_IMAGE_PATH")
            if (!path.isNullOrEmpty()) {
                val file = File(path)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        handleCapturedBitmap(bitmap)
                        val uri = Uri.fromFile(file)
                        fileChooserCallback?.onReceiveValue(arrayOf(uri))
                        fileChooserCallback = null
                        return@registerForActivityResult
                    }
                }
            }
        }
        fileChooserCallback?.onReceiveValue(null)
        fileChooserCallback = null
    }

    // Gallery launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedUri(uri)
            fileChooserCallback?.onReceiveValue(arrayOf(uri))
        } else {
            fileChooserCallback?.onReceiveValue(null)
        }
        fileChooserCallback = null
    }

    // Permission request launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (!cameraGranted) {
            Toast.makeText(this, "Camera permission recommended for lesion scanning", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableMaxRefreshRate()

        val prefs = getSharedPreferences("derma_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_admin", false)) {
            val intent = Intent(this, com.dermatrace.app.admin.ui.AdminActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure status bar and navigation bar are dark with white text/icons on every device
        try {
            window.statusBarColor = android.graphics.Color.BLACK
            window.navigationBarColor = android.graphics.Color.BLACK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(
                    0,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = 0
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val params = window.attributes
                params.layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                window.attributes = params
            }
        } catch (_: Exception) {
            // Graceful fallback
        }

        try {
            requestNeededPermissions()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            initServices()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            setupWebView()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun initServices() {
        db = DermaDatabase.getInstance(this)
        analyzer = TfliteLesionAnalyzer(this)
        bridge = DermaTraceBridge(this, analyzer, db)
    }

    private fun setupWebView() {
        val webView = binding.webView
        val settings = webView.settings

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
        }

        WebView.setWebContentsDebuggingEnabled(true)
        webView.setBackgroundColor(android.graphics.Color.BLACK)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.addJavascriptInterface(bridge, "AndroidBridge")
        webView.addJavascriptInterface(bridge, "DermaBridge")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress >= 100) {
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback
                launchCameraOrGalleryChooser()
                return true
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                return super.onConsoleMessage(consoleMessage)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
                // Notify frontend of bridge readiness
                evaluateJavascript("window.onAndroidBridgeReady && window.onAndroidBridgeReady();")
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                binding.progressBar.visibility = View.GONE
            }
        }

        // Load the HTML front end
        webView.loadUrl("file:///android_asset/web/index.html")
    }

    private fun launchCameraOrGalleryChooser() {
        launchCamera()
    }

    fun launchCamera() {
        launchCameraWithGhost(null)
    }

    fun launchCameraWithGhost(explicitGhostPath: String?) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            isInternalLaunch = true
            val intent = Intent(this, CameraActivity::class.java)
            try {
                if (!explicitGhostPath.isNullOrEmpty() && File(explicitGhostPath).exists()) {
                    intent.putExtra("EXTRA_GHOST_PATH", explicitGhostPath)
                } else {
                    val activeLesionId = bridge.getActiveLesionId()
                    val visits = db.getVisitsForLesion(activeLesionId)
                    val latestVisit = visits.maxByOrNull { it.timestamp }
                    if (latestVisit != null && latestVisit.imagePath.isNotBlank() && File(latestVisit.imagePath).exists()) {
                        intent.putExtra("EXTRA_GHOST_PATH", latestVisit.imagePath)
                    }
                }
            } catch (_: Exception) {}
            cameraScanLauncher.launch(intent)
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }

    fun launchGallery() {
        isInternalLaunch = true
        pickImageLauncher.launch("image/*")
    }

    override fun onResume() {
        super.onResume()
        if (isInternalLaunch) {
            isInternalLaunch = false
            return
        }
        if (BiometricSecurityManager.isBiometricLockEnabled(this) &&
            BiometricSecurityManager.isHardwareSupported(this) &&
            !isAuthenticating &&
            !isSessionAuthenticated
        ) {
            isAuthenticating = true
            BiometricSecurityManager.authenticate(
                activity = this,
                title = "DermaTrace Patient Authentication",
                subtitle = "Confirm biometric identity to access confidential lesion records",
                onSuccess = {
                    isAuthenticating = false
                    isSessionAuthenticated = true
                },
                onError = { err ->
                    isAuthenticating = false
                    Toast.makeText(this, "Biometric security prompt: $err", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isInternalLaunch) {
            isSessionAuthenticated = false
        }
    }

    fun markSessionAuthenticated() {
        isSessionAuthenticated = true
    }

    private fun handleCapturedBitmap(bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val base64 = "data:image/jpeg;base64," + Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        evaluateJavascript("window.onImageCaptured && window.onImageCaptured('$base64');")
    }

    private fun handleSelectedUri(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
                handleCapturedBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Could not open image", Toast.LENGTH_SHORT).show()
        }
    }

    fun evaluateJavascript(script: String) {
        runOnUiThread {
            binding.webView.evaluateJavascript(script, null)
        }
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private var swipeStartTime = 0L
    private var isSwipeConsumed = false
    private var lastSwipeTriggerTime = 0L

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && ::binding.isInitialized) {
            val now = System.currentTimeMillis()
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    swipeStartX = ev.rawX
                    swipeStartY = ev.rawY
                    swipeStartTime = now
                    isSwipeConsumed = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isSwipeConsumed && (now - lastSwipeTriggerTime > 300)) {
                        val dx = ev.rawX - swipeStartX
                        val dy = ev.rawY - swipeStartY
                        val absDx = Math.abs(dx)
                        val absDy = Math.abs(dy)

                        // Trigger when horizontal displacement is prominent (> 85px) and largely horizontal
                        if (absDx >= 85f && absDx > absDy * 1.35f) {
                            isSwipeConsumed = true
                            lastSwipeTriggerTime = now
                            val dir = if (dx < 0) "next" else "prev"
                            evaluateJavascript("window.onNativeSwipe && window.onNativeSwipe('$dir');")
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!isSwipeConsumed && (now - lastSwipeTriggerTime > 300)) {
                        val dx = ev.rawX - swipeStartX
                        val dy = ev.rawY - swipeStartY
                        val absDx = Math.abs(dx)
                        val absDy = Math.abs(dy)
                        val dt = now - swipeStartTime

                        // Quick flick / fling or finger release (>= 40px, or short duration < 320ms and >= 30px)
                        if ((absDx >= 40f && absDx > absDy * 1.15f) || (dt < 320 && absDx >= 30f && absDx > absDy * 1.15f)) {
                            isSwipeConsumed = true
                            lastSwipeTriggerTime = now
                            val dir = if (dx < 0) "next" else "prev"
                            evaluateJavascript("window.onNativeSwipe && window.onNativeSwipe('$dir');")
                        }
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    isSwipeConsumed = false
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun enableMaxRefreshRate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setPreferMinimalPostProcessing(true)
                val display = display
                val modes = display?.supportedModes
                val maxMode = modes?.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    val params = window.attributes
                    params.preferredDisplayModeId = maxMode.modeId
                    window.attributes = params
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val display = windowManager.defaultDisplay
                val modes = display?.supportedModes
                val maxMode = modes?.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    val params = window.attributes
                    params.preferredDisplayModeId = maxMode.modeId
                    window.attributes = params
                }
            }
        } catch (_: Exception) {
            // Graceful fallback
        }
    }
}
