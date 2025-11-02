package dji.samplev5.aircraft

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var captureButton: Button

    private val neededPermissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.INTERNET,
        Manifest.permission.VIBRATE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val denied = result.filterValues { !it }.keys
            if (denied.isNotEmpty()) {
                updateStatus("Permissions denied: $denied")
            } else {
                updateStatus("All permissions granted")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        captureButton = findViewById(R.id.captureButton)

        captureButton.setOnClickListener { capturePhoto() }
        requestMissingPermissions()
    }

    private fun requestMissingPermissions() {
        val missing = neededPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            updateStatus("All permissions already granted")
        }
    }

    private fun getCameraInstance(): Camera? {
        val product = DJICameraSampleApp.getProductInstance()
        if (product is Aircraft) {
            return product.camera
        }
        updateStatus("No aircraft connected")
        return null
    }

    private fun capturePhoto() {
        val camera = getCameraInstance() ?: return

        camera.setMode(CameraMode.SHOOT_PHOTO) { error ->
            if (error == null) {
                camera.startShootPhoto(
                    CameraShootPhotoMode.SINGLE,
                    CommonCallbacks.CompletionCallback { shootError ->
                        if (shootError == null) {
                            updateStatus("Photo capture triggered")
                        } else {
                            updateStatus("Failed to trigger photo: ${shootError.description}")
                        }
                    }
                )
            } else {
                updateStatus("Failed to switch mode: ${error.description}")
            }
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread { statusText.text = message }
    }

}