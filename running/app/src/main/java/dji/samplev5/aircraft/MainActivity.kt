package dji.samplev5.aircraft

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dji.common.camera.SettingsDefinitions
import dji.common.error.DJIError
import dji.common.product.Model
import dji.common.util.CommonCallbacks
import dji.sdk.base.BaseProduct
import dji.sdk.camera.Camera
import dji.sdk.media.MediaFile
import dji.sdk.media.MediaManager
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import java.io.File


class MainActivity : AppCompatActivity() {
    // Companion object for constants
    companion object {
        private const val TAG = "DJI_PhotoApp"
        private const val REQUEST_PERMISSION_CODE = 12345
    }

    // UI Elements
    private lateinit var captureButton: Button
    private lateinit var statusTextView: TextView

    // Handler for posting to the main thread
    private val uiHandler = Handler(Looper.getMainLooper())

    // DJI SDK Objects
    private var mediaManager: MediaManager? = null
    private var product: BaseProduct? = null

    // Listener for new media files created on the drone
    private val newFileListener = MediaManager.NewFileListener { mediaFile ->
        Log.d(TAG, "New file generated: ${mediaFile.fileName}")
        updateStatus("New file detected: ${mediaFile.fileName}")
        downloadFile(mediaFile)
    }

    // Listener for DJI Product connection changes
    private val productConnectionListener = object : DJISDKManager.SDKManagerCallback {
        override fun onRegister(error: DJIError?) {
            if (error == null) {
                Log.i(TAG, "SDK Registration Success!")
                DJISDKManager.getInstance().startConnectionToProduct()
            } else {
                Log.e(TAG, "SDK Registration Failed: ${error.description}")
                showToast("SDK Registration Failed: ${error.description}")
            }
        }

        override fun onProductDisconnect() {
            Log.d(TAG, "Product Disconnected")
            product = null
            notifyStatusChange()
        }

        override fun onProductConnect(baseProduct: BaseProduct?) {
            Log.d(TAG, "Product Connected")
            product = baseProduct
            notifyStatusChange()
        }

        override fun onProductChanged(baseProduct: BaseProduct?) {
            Log.d(TAG, "Product Changed")
            product = baseProduct
            notifyStatusChange()
        }

        override fun onComponentChange(
            componentKey: BaseProduct.ComponentKey?,
            oldComponent: dji.sdk.base.BaseComponent?,
            newComponent: dji.sdk.base.BaseComponent?
        ) {
            newComponent?.let {
                it.setComponentListener { connected ->
                    Log.d(TAG, "Component ${it.javaClass.simpleName} connected: $connected")
                    notifyStatusChange()
                }
            }
        }
        // ... Other overrides can be added if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()

        captureButton = findViewById(R.id.captureButton)
        statusTextView = findViewById(R.id.statusTextView)

        // Set up the button click listener
        captureButton.setOnClickListener { takePicture() }

        // The product listener is often initialized in a separate Application class,
        // but for simplicity, we do it here.
        DJISDKManager.getInstance().sdkManagerCallback = productConnectionListener
    }

    private fun notifyStatusChange() {
        if (product != null && product!!.isConnected) {
            val modelName = product!!.model?.displayName ?: "Unknown Drone"
            updateStatus("Status: $modelName Connected")

            // Initialize Media Manager once connected
            initMediaManager()

            // Enable the capture button
            uiHandler.post { captureButton.isEnabled = true }

        } else {
            updateStatus("Status: Disconnected. Please connect drone.")
            // Disable the capture button
            uiHandler.post { captureButton.isEnabled = false }
        }
    }


    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                showToast("Permissions Granted!")
            } else {
                showToast("Permissions Denied! The app cannot save photos.")
            }
        }
    }

    /**
     *  Step 1: Get Camera Instance
     *  Step 2: Set Camera Mode to SHOOT_PHOTO
     *  Step 3: Trigger the shutter
     */
    private fun takePicture() {
        val camera = getCameraInstance() ?: run {
            showToast("Camera not available!")
            return
        }

        // Set camera mode to Single Shot Photo
        camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO) { djiError ->
            if (djiError == null) {
                updateStatus("Mode set to Shoot Photo")
                // After mode is set, take the picture
                camera.startShootPhoto { djiError1 ->
                    if (djiError1 == null) {
                        showToast("Picture Taken! Awaiting file...")
                        updateStatus("Shutter triggered. Waiting for file...")
                    } else {
                        showToast("Failed to take picture: ${djiError1.description}")
                        updateStatus("Error: ${djiError1.description}")
                    }
                }
            } else {
                showToast("Failed to set camera mode: ${djiError.description}")
                updateStatus("Error: ${djiError.description}")
            }
        }
    }

    /**
     * Initializes the MediaManager and sets up a listener for new files.
     */
    private fun initMediaManager() {
        val camera = getCameraInstance() ?: run {
            Log.e(TAG, "Cannot init MediaManager. Camera is null.")
            return
        }

        mediaManager = camera.mediaManager?.also {
            // Important: remove any previous listeners to avoid duplicates
            it.removeNewFileListener(newFileListener)
            it.addNewFileListener(newFileListener)
            Log.d(TAG, "Media Manager Initialized.")
        }
    }

    /**
     * Downloads the specified MediaFile from the drone's SD card.
     */
    private fun downloadFile(mediaFile: MediaFile) {
        val destDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path, "DJI_Photo_App").apply {
            if (!exists()) mkdirs()
        }

        updateStatus("Downloading...")
        showToast("Starting download...")

        mediaFile.fetchFileData(destDir, null, object : CommonCallbacks.CompletionCallbackWith<String> {
            override fun onSuccess(filePath: String) {
                showToast("File saved!")
                updateStatus("Saved: ${File(filePath).name}")
                Log.d(TAG, "File saved successfully at: $filePath")
            }

            override fun onFailure(djiError: DJIError) {
                showToast("Download failed: ${djiError.description}")
                updateStatus("Error downloading: ${djiError.description}")
                Log.e(TAG, "Download failed: ${djiError.description}")
            }
        })
    }

    // Helper function to get the camera instance from the connected product
    private fun getCameraInstance(): Camera? = (product as? Aircraft)?.camera

    // Helper to show toasts on the UI thread
    private fun showToast(message: String) {
        uiHandler.post { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
    }

    // Helper to update the status text view on the UI thread
    private fun updateStatus(message: String) {
        uiHandler.post { statusTextView.text = message }
    }

    override fun onDestroy() {
        // Clean up listeners to prevent memory leaks
        mediaManager?.removeNewFileListener(newFileListener)
        DJISDKManager.getInstance().sdkManagerCallback = null
        super.onDestroy()
    }
}