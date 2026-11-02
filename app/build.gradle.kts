plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.oliver.videoeditor"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.oliver.videoeditor"
        minSdk = 29; targetSdk = 34; versionCode = 1; versionName = "1.0"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    val m = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$m")
    implementation("androidx.media3:media3-ui:$m")
    implementation("androidx.media3:media3-transformer:$m")
    implementation("androidx.media3:media3-effect:$m")
    implementation("androidx.media3:media3-common:$m")
    implementation("com.google.code.gson:gson:2.11.0")
}
