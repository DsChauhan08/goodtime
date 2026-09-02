import com.android.build.api.variant.BuildConfigField
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps =
    Properties().apply {
        if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
    }

fun signingValue(
    propKey: String,
    envKey: String,
): String? = keystoreProps.getProperty(propKey) ?: System.getenv(envKey)

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidxBaselineProfile)
    alias(libs.plugins.kmp.app.version)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

android {
    namespace = "com.apps.adrcotfas.mytime.app"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.apps.adrcotfas.mytime"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        flavorDimensions += "distribution"
        productFlavors {
            create("google") {
                dimension = "distribution"
            }
            create("fdroid") {
                dimension = "distribution"
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Google Drive API dependencies have conflicting META-INF files
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    val storeFilePath = signingValue("storeFile", "RELEASE_STORE_FILE")
    signingConfigs {
        if (storeFilePath != null) {
            create("release") {
                storeFile = rootProject.file(storeFilePath)
                storePassword = signingValue("storePassword", "RELEASE_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "RELEASE_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig =
                if (storeFilePath != null) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
        getByName("debug") {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            isUniversalApk = true
        }
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
}

androidComponents {
    onVariants { variant ->
        val isFdroid = variant.flavorName == "fdroid"
        variant.buildConfigFields?.apply {
            put("IS_FDROID", BuildConfigField("boolean", isFdroid.toString(), null))
        }
    }
}

dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.work.runtime.ktx)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    baselineProfile(projects.baselineProfile)

    // Google Play distribution
    add("googleImplementation", libs.app.update.ktx)
    add("googleImplementation", libs.review.ktx)
    add("googleImplementation", libs.google.play.auth)
    add("googleImplementation", libs.google.play.auth.credentials)
    add("googleImplementation", libs.google.api.client)
    add("googleImplementation", libs.google.drive)
    add("googleImplementation", libs.google.id)
    add("googleImplementation", libs.coroutines.play.services)
}
