plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") // 加入 Google Services 外掛
}

android {
    namespace = "com.example.comps313fproject"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.comps313fproject"
        minSdk = 30
        targetSdk = 30
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
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.google.guava:guava:31.1-android")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.rmtheis:tess-two:9.0.0")
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("androidx.work:work-runtime-ktx:2.7.0")
    implementation("com.google.android.gms:play-services-location:18.0.0")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.android.volley:volley:1.2.1")

}

