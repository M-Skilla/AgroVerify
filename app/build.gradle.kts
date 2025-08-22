plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.group.agroverify"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.group.agroverify"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Add 16 KB page size support
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Room database dependencies
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-rxjava3:2.6.1")

    // Additional dependencies for async operations
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    // SwipeRefreshLayout dependency
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // https://mvnrepository.com/artifact/com.airbnb.android/lottie
    implementation("com.airbnb.android:lottie:6.6.7")

    // CameraX dependencies for QR scanning
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // ML Kit for barcode scanning
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // Guava for ListenableFuture (required by CameraX)
    implementation("com.google.guava:guava:32.1.3-android")
}