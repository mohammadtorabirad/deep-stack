plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dji.samplev5.aircraft"
    compileSdk = 36

    defaultConfig {
        applicationId = "dji.samplev5.aircraft"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += setOf(
                "lib/arm64-v8a/libdjivideo.so",
                "lib/armeabi-v7a/libdjivideo.so",
                "lib/arm64-v8a/libDJIWaypointV2Core.so",
                "lib/armeabi-v7a/libDJIWaypointV2Core.so",
                "lib/arm64-v8a/libDJIMOPipeline.so",
                "lib/armeabi-v7a/libDJIMOPipeline.so",
                "lib/arm64-v8a/libDJIUpgradeCore.so",
                "lib/armeabi-v7a/libDJIUpgradeCore.so"
            )
        }

        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }

}


dependencies {

    implementation(libs.dji.sdk.v5)
//    implementation(libs.dji.sdk.provided.impl)
    compileOnly(libs.dji.sdk.v5.provided)
    implementation(libs.dji.sdk.network)

    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}