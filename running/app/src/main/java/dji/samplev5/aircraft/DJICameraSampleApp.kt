package dji.samplev5.aircraft

import android.app.Application
import android.content.Context
import android.util.Log
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback
class DJICameraSampleApp : Application() {
    override fun onCreate() {
        super.onCreate()

        SDKManager.getInstance().init(this, object : SDKManagerCallback {
            override fun onInitProcess(event: DJISDKInitEvent, totalProcess: Int) {
                if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                    SDKManager.getInstance().registerApp()
                }
            }

            override fun onRegisterSuccess() {
                Log.i("DJI_APP", "✅ DJI SDK Register Success")
            }

            override fun onRegisterFailure(error: IDJIError?) {
                Log.e("DJI_APP", "❌ DJI SDK Register Failed: ${error?.errorCode()} - ${error?.description()}")
            }

            override fun onProductDisconnect(productId: Int) {
                TODO("Not yet implemented")
            }

            override fun onProductConnect(productId: Int) {
                TODO("Not yet implemented")
            }

            override fun onProductChanged(productId: Int) {
                TODO("Not yet implemented")
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                TODO("Not yet implemented")
            }

        })
    }
}