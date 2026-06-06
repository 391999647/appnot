plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.application")
    id("com.google.devtools.ksp")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    // TODO: HarmonyOS 需配置自定义目标或使用 Kuikly 提供的 Harmony 插件
    // ohosArm64()

    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "ntnotes.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("com.tencent.kuikly-open:core:2.4.0-2.0.21")
                implementation("com.tencent.kuikly-open:core-annotations:2.4.0-2.0.21")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("com.tencent.kuikly-open:core:2.4.0-2.0.21")
                implementation("com.tencent.kuikly-open:core-render-android:2.4.0-2.0.21")
                implementation("androidx.activity:activity-ktx:1.8.2")
                implementation("androidx.security:security-crypto:1.0.0")
                implementation("androidx.multidex:multidex:2.0.1")
                implementation("androidx.recyclerview:recyclerview:1.3.2")
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        // val ohosArm64Main by creating { dependsOn(commonMain) }
        val jsMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

dependencies {
    add("kspAndroid", "com.tencent.kuikly-open:core-ksp:2.4.0-2.0.21")
    add("kspIosX64", "com.tencent.kuikly-open:core-ksp:2.4.0-2.0.21")
    add("kspIosArm64", "com.tencent.kuikly-open:core-ksp:2.4.0-2.0.21")
    add("kspIosSimulatorArm64", "com.tencent.kuikly-open:core-ksp:2.4.0-2.0.21")
    add("kspJs", "com.tencent.kuikly-open:core-ksp:2.4.0-2.0.21")
    // add("kspOhosArm64", "com.tencent.kuikly-open:core-ksp:2.4.0-2.0.21")
}

android {
    namespace = "com.noteapp"
    compileSdk = 34
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.noteapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        multiDexEnabled = true
    }
    buildTypes {
        debug {
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "IS_DEBUG", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "IS_DEBUG", "false")
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
    sourceSets {
        named("main") {
            assets.srcDirs("src/commonMain/assets")
        }
    }
}
