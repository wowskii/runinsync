import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.runinsync"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.runinsync"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        val localPropertiesFile = rootProject.file("local.properties") // Use rootProject.file for better pathing
        var apiKeyFromProperties: String? = null

        if (localPropertiesFile.exists()) {
            val properties = Properties()
            localPropertiesFile.inputStream().use { input ->
                properties.load(input)
            }
            apiKeyFromProperties = properties.getProperty("API_KEY")
        }

        if (apiKeyFromProperties == null) {
            // Option 1: Provide a default placeholder (for debug builds, less secure)
            // apiKeyFromProperties = "YOUR_DEFAULT_DEBUG_API_KEY_PLACEHOLDER"
            // println("Warning: API_KEY not found in local.properties. Using default.")

            // Option 2: Fail the build if the key is mandatory (recommended for release)
            throw GradleException("API_KEY not found in local.properties. Please create this file and add the API_KEY property.")
        }

        // IMPORTANT: The value must be a valid Java String literal, so it needs quotes.
        buildConfigField("String", "API_KEY", apiKeyFromProperties)

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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.lifecycle.viewmodel.compose.v262)
    implementation(libs.exoplayer)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)

    // Retrofit
    implementation(libs.retrofit) // Use the latest version

    // Gson Converter (or Moshi, or other)
    implementation(libs.converter.gson) // Use the latest version

    // OkHttp (Retrofit includes it, but you might want to specify it for its BOM or interceptors)
    implementation(libs.okhttp) // Or use OkHttp BOM
    implementation(libs.logging.interceptor) // Useful for debugging API calls

    // Kotlin Coroutines (already likely in your project for viewModelScope)
    implementation(libs.kotlinx.coroutines.android) // Use latest
}