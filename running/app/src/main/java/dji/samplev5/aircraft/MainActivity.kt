package dji.samplev5.aircraft

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.camera.CameraMode
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.SDKManager


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
//        Manifest.permission.BLUETOOTH,
//        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.INTERNET,
        Manifest.permission.VIBRATE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.ACCESS_NETWORK_STATE
            .apply {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    plus(arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    ))
                } else {
                    plus(arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ))
                }
            }
//        Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
//        Manifest.permission.CAMERA,
//        Manifest.permission.RECORD_AUDIO
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

        captureButton.setOnClickListener { captureSinglePhoto() }
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

    private fun captureSinglePhoto() {

        if (!SDKManager.getInstance().isRegistered) {
            updateStatus("DJI SDK not registered yet")
            return
        }

        val keyManager = KeyManager.getInstance()
        if (keyManager == null) {
            updateStatus("❌ KeyManager not available")
            return
        }
        ensurePhotoMode(keyManager)
    }

    private fun ensurePhotoMode(keyManager: KeyManager) {
        val modeKey = KeyTools.createKey(CameraKey.KeyCameraMode, ComponentIndexType.LEFT_OR_MAIN)

        keyManager.setValue(modeKey, CameraMode.PHOTO_NORMAL, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                updateStatus("Camera mode set to photo normal")
                triggerShutter(keyManager)

            }

            override fun onFailure(error: IDJIError) {
                updateStatus("Camera mode set failed")

                Log.e("DJI", "❌ Error setting camera mode: " + error.description())
            }
        })

    }


    private fun triggerShutter(keyManager: KeyManager) {
        val actionKey = KeyTools.createCameraKey(
            CameraKey.KeyStartShootPhoto,
            ComponentIndexType.LEFT_OR_MAIN,
            CameraLensType.UNKNOWN
        )


        keyManager.performAction(
            actionKey,
            object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
                override fun onSuccess(result: EmptyMsg) {
                    Log.i("DJI", "✅ Photo capture started")
                    updateStatus("Photo captured!!!")
                }

                override fun onFailure(error: IDJIError) {
                    Log.e("DJI", "❌ Error capturing photo: ${error.description()}")
                    updateStatus("Capture failed: ${error.description()}")
                }
            })

    }

    private fun updateStatus(message: String) {
        runOnUiThread { statusText.text = message }
    }

}