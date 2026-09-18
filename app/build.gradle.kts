plugins { id("com.android.application") }

android {
    namespace="com.norbor.myvpn"
    compileSdk=36
    defaultConfig {
        applicationId="com.norbor.myvpn"
        minSdk=26
        targetSdk=36
        versionCode=1
        versionName="1.0"
    }
    compileOptions {
        sourceCompatibility=JavaVersion.VERSION_17
        targetCompatibility=JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled=true
    }
}
dependencies {
    implementation("com.wireguard.android:tunnel:1.0.20260102")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
