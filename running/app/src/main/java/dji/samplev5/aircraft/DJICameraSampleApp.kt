package dji.samplev5.aircraft

class DJICameraSampleApp : Application() {
    companion object {
        private const val TAG = "DJICameraSampleApp"

        var product: BaseProduct? = null
            private set

        fun getProductInstance(): BaseProduct? = product
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        DJISDKManager.getInstance().registerApp(this, object : DJISDKManager.SDKManagerCallback {
            override fun onRegister(djiError: DJIError?) {
                if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                    Log.d(TAG, "Registration successful")
                    DJISDKManager.getInstance().startConnectionToProduct()
                } else {
                    Log.e(TAG, "Registration failed: ${djiError?.description}")
                }
            }

            override fun onProductDisconnect() {
                Log.d(TAG, "Product disconnected")
                product = null
            }

            override fun onProductConnect(baseProduct: BaseProduct?) {
                Log.d(TAG, "Product connected: $baseProduct")
                product = baseProduct
            }

            override fun onProductChanged(baseProduct: BaseProduct?) {
                Log.d(TAG, "Product changed: $baseProduct")
                product = baseProduct
            }

            override fun onComponentChange(
                componentKey: dji.sdk.base.BaseProduct.ComponentKey?,
                oldComponent: dji.sdk.base.BaseComponent?,
                newComponent: dji.sdk.base.BaseComponent?
            ) {
                Log.d(TAG, "Component changed: $componentKey")
            }

            override fun onInitProcess(djisdkInitEvent: DJISDKInitEvent?, i: Int) {
                Log.d(TAG, "Init process: $djisdkInitEvent $i")
            }

            override fun onDatabaseDownloadProgress(l: Long, l1: Long) {}
        })
    }
}