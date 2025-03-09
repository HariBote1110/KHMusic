plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.khmusic"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.khmusic"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" //使用したいバージョン
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-runtime-ktx:2.7.7")
    implementation(libs.androidx.media3.common.ktx)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Compose 必要な依存関係
    implementation("androidx.compose.ui:ui:1.6.6") // バージョンは適宜変更
    implementation("androidx.compose.ui:ui-graphics:1.6.6")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.6")
    implementation("androidx.compose.material3:material3:1.3.1") // Material Design 3
    implementation("androidx.activity:activity-compose:1.9.0") // 必須
    implementation("androidx.compose.runtime:runtime-livedata:1.6.6") // LiveData との連携
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2") // ViewModel との連携
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.6")  // プレビュー用
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.6")

    //Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0") // または ksp を使用
    // Coil
    implementation("io.coil-kt:coil-compose:2.6.0")
    //
    implementation("androidx.navigation:navigation-compose:2.7.7")//composeでの画面遷移に必要なライブラリ
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")//viewModelScopeを使うために必要

    implementation("androidx.compose.material:material-icons-extended:1.6.6") // 追加
}