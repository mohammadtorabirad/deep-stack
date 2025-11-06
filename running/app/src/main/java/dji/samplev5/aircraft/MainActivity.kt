package dji.samplev5.aircraft

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import dji.v5.common.callback.CommonCallbacks

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

        captureButton.setOnClickListener { takePhoto() }
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


    private fun takePhoto() {
        val cameraMgr = CameraManager.getInstance()
        val camera = cameraMgr.currentCamera
        if (camera == null) {
            Toast.makeText(this, "No camera connected", Toast.LENGTH_SHORT).show()
            return
        }

        cameraMgr.setPhotoShootMode(PhotoShootMode.SINGLE, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                cameraMgr.startShootPhoto(object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        Toast.makeText(this@MainActivity, "📸 Photo captured!", Toast.LENGTH_SHORT).show()
                    }
                    override fun onFailure(error: DJIError) {
                        Toast.makeText(this@MainActivity, "Error: ${error.description}", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onFailure(error: DJIError) {
                Toast.makeText(this@MainActivity, "Mode set failed: ${error.description}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateStatus(message: String) {
        runOnUiThread { statusText.text = message }
    }

}