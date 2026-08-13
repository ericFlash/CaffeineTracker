plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// 版本号管理：每次提交/构建对应唯一版本
// versionCode = git 提交总数（单调递增，满足 Android 覆盖安装要求）
// versionName = 主.次.提交数+提交短哈希
fun runGit(vararg args: String): String = try {
    val p = ProcessBuilder("git", *args)
        .redirectErrorStream(false)
        .start()
    val raw = p.inputStream.readBytes().toString(Charsets.UTF_8)
    p.waitFor()
    raw.trim()
} catch (e: Exception) {
    ""
}

val gitCommitCount: String = runGit("rev-list", "--count", "HEAD").ifBlank { "0" }
val gitShortSha: String = runGit("rev-parse", "--short=7", "HEAD").ifBlank { "dev" }
val versionMajor = 1
val versionMinor = 0

android {
    namespace = "com.caffeine.tracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.caffeine.tracker"
        minSdk = 26
        targetSdk = 35
        versionCode = gitCommitCount.toIntOrNull() ?: 1
        versionName = "$versionMajor.$versionMinor.${gitCommitCount.takeIf { it.isNotBlank() } ?: "0"}+$gitShortSha"
    }

    // 固定签名：所有构建（debug/release）使用仓库内的同一个 keystore，
    // 保证本地 IDE 与 GitHub Actions 产出的 APK 签名一致，可覆盖安装。
    signingConfigs {
        create("fixed") {
            storeFile = file("../keystore/release.p12")
            storePassword = "android"
            keyAlias = "1"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fixed")
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("fixed")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.11.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.activity:activity-ktx:1.9.3")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.53.1")
    ksp("com.google.dagger:hilt-android-compiler:2.53.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Glance Widget
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-remoteviews:1.1.0")
}
