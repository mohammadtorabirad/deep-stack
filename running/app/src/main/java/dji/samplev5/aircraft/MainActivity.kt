package dji.samplev5.aircraft

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.camera.CameraMode
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.SDKManager
import android.os.Handler

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var captureButton: Button

    private var isDroneConnected = false

    private val handler = Handler(Looper.getMainLooper())

    private var detectedLens: CameraLensType? = null
    private var detectedComponent: ComponentIndexType? = null
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
                    plus(
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO
                        )
                    )
                } else {
                    plus(
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                    )
                }
            }
//        Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
//        Manifest.permission.CAMERA,
//        Manifest.permission.RECORD_AUDIO
    )

    private fun monitorConnection() {
        // چک کردن مداوم وضعیت اتصال
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkDroneConnection()
                handler.postDelayed(this, 2000)
            }
        }, 2000)
    }
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
        monitorConnection()
    }

    private fun requestMissingPermissions() {
        val missing = neededPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
        }

    }

    private fun checkDroneConnection() {
        val connectionKey = KeyTools.createKey(FlightControllerKey.KeyConnection)
        KeyManager.getInstance().getValue(connectionKey, object : CommonCallbacks.CompletionCallbackWithParam<Boolean> {
            override fun onSuccess(connected: Boolean?) {
                isDroneConnected = connected == true
                runOnUiThread {
                    updateStatus(if (isDroneConnected) "✅ پهپاد متصل است" else "⏳ در انتظار اتصال...")
                    detectAvailableCamera()
                }
            }

            override fun onFailure(error: IDJIError) {
                isDroneConnected = false
                runOnUiThread {
                    updateStatus("❌ خطا در بررسی اتصال: ${error.description()}")
                }
            }
        })
    }

    private fun detectAvailableCamera() {
        // ✅ تست لنزهای مختلف
        val testLenses = listOf(
            CameraLensType.CAMERA_LENS_ZOOM,
            CameraLensType.CAMERA_LENS_WIDE,
            CameraLensType.CAMERA_LENS_THERMAL,
                    CameraLensType.CAMERA_LENS_ZOOM,
            CameraLensType.CAMERA_LENS_WIDE,
            CameraLensType.CAMERA_LENS_THERMAL,
            CameraLensType.CAMERA_LENS_MS_G,
            CameraLensType.CAMERA_LENS_MS_R,
            CameraLensType.CAMERA_LENS_MS_RE,
            CameraLensType.CAMERA_LENS_MS_NIR,
            CameraLensType.CAMERA_LENS_MS_NDVI,
            CameraLensType.CAMERA_LENS_RGB,
            CameraLensType.CAMERA_LENS_PCD,
            CameraLensType.CAMERA_LENS_DEFAULT,
            CameraLensType.UNKNOWN

        )

        val testComponents = listOf(
            ComponentIndexType.LEFT_OR_MAIN,
            ComponentIndexType.UP,
            ComponentIndexType.LEFT_OR_MAIN,
            ComponentIndexType.RIGHT,
            ComponentIndexType.UP,
            ComponentIndexType.INDEX_3,
            ComponentIndexType.UP_TYPE_C,
            ComponentIndexType.UP_TYPE_C_EXT_ONE,
            ComponentIndexType.INDEX_6,
            ComponentIndexType.FPV,
            ComponentIndexType.INDEX_8,
            ComponentIndexType.INDEX_9,
            ComponentIndexType.INDEX_10,
            ComponentIndexType.INDEX_11,
            ComponentIndexType.INDEX_12,
            ComponentIndexType.AGGREGATION,
            ComponentIndexType.VISION_ASSIST,
            ComponentIndexType.PORT_1,
            ComponentIndexType.PORT_2,
            ComponentIndexType.PORT_3,
            ComponentIndexType.PORT_4,
            ComponentIndexType.PORT_5,
            ComponentIndexType.PORT_6,
            ComponentIndexType.PORT_7,
            ComponentIndexType.PORT_8

            )

        // ✅ چک کردن اولین ترکیب موفق
        checkNextCamera(testComponents, testLenses, 0, 0)
    }

    private fun checkNextCamera(
        components: List<ComponentIndexType>,
        lenses: List<CameraLensType>,
        compIndex: Int,
        lensIndex: Int
    ) {
        if (compIndex >= components.size) {
            Log.e("DJI", "❌ No camera detected in simulator!")
            runOnUiThread {
                updateStatus("❌ هیچ دوربینی پیدا نشد - شبیه‌ساز را تنظیم کنید")
            }
            return
        }

        if (lensIndex >= lenses.size) {
            checkNextCamera(components, lenses, compIndex + 1, 0)
            return
        }

        val component = components[compIndex]
        val lens = lenses[lensIndex]

        val modeKey = KeyTools.createKey(
            CameraKey.KeyCameraMode,
            component
        )

        Log.d("DJI", "🔍 Testing camera - Component: $component, Lens: $lens")

        KeyManager.getInstance().getValue(modeKey, object : CommonCallbacks.CompletionCallbackWithParam<CameraMode> {
            override fun onSuccess(mode: CameraMode?) {
                Log.i("DJI", "✅ Camera detected! Component: $component, Lens: $lens, Mode: $mode")
                detectedComponent = component
                detectedLens = lens

                runOnUiThread {
                    updateStatus("✅ دوربین شناسایی شد - آماده عکس‌گیری")
                    captureButton.isEnabled = true
                }
            }

            override fun onFailure(error: IDJIError) {
                // ✅ اگر خطای 255 (handler not found) بود، تست بعدی
                if (error.errorCode() == "255") {
                    Log.d("DJI", "❌ No camera at $component/$lens, trying next...")
                    checkNextCamera(components, lenses, compIndex, lensIndex + 1)
                } else {
                    Log.e("DJI", "❌ Camera detection error: ${error.description()}")
                }
            }
        })
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

        keyManager.setValue(
            modeKey,
            CameraMode.PHOTO_NORMAL,
            object : CommonCallbacks.CompletionCallback {
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
            CameraLensType.CAMERA_LENS_ZOOM
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
                    updateStatus("Capture failed: ${error.errorCode()}")
                }
            })

    }

    private fun updateStatus(message: String) {
        runOnUiThread { statusText.text = message }
    }

}