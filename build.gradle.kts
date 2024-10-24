// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply true
    alias(libs.plugins.kotlin.android) apply true
}

android {
    namespace = "com.example.fast2"  // 본인의 패키지명으로 수정
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.fast2"  // 본인의 패키지명으로 수정
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    // ... 나머지 설정들
}