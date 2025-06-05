plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.gamifylife"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gamifylife"
        minSdk = 24
        targetSdk = 35
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
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true // Zakładam, że używasz ViewBinding
    }
}

dependencies {
    // AndroidX & Material Design
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.activity)
    implementation(libs.preferencektx)

    // Testy
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline) // Dodaj to

    // Glide (ładowanie obrazów)
    implementation(libs.glide)
    annotationProcessor(libs.glideCompiler) // Użyj annotationProcessor dla kompilatora Glide

    // Google Services & Ads
    implementation(libs.playserviceads)
    implementation(libs.credentials) // AndroidX Credentials
    implementation(libs.credentials.play.services.auth) // AndroidX Credentials Play Services Auth
    implementation(libs.googleid) // Google Identity Services library

    // Firebase
    implementation(platform(libs.firebase.bom)) // Importuj Firebase BoM
    implementation(libs.firebase.auth)          // Firebase Authentication (wersja zarządzana przez BoM)
    implementation(libs.firebase.firestore)     // Cloud Firestore (wersja zarządzana przez BoM)
    implementation(libs.firebase.storage)       // Firebase Storage (wersja zarządzana przez BoM)
    implementation(libs.firebase.appcheck.playintegrity) // Firebase App Check (wersja zarządzana przez BoM)

    // Github
    implementation(libs.mpandroidchart)

    // Jeśli masz inne biblioteki, dodaj je tutaj
}