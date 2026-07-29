import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.mikepenz.aboutlibraries)
}

kotlin {
    android {
        namespace = "com.apps.adrcotfas.mytime"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        androidResources.enable = true

        withHostTestBuilder {
        }.configure {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlin.time.ExperimentalTime")
            }
        }

        androidMain.dependencies {
            api(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.koin.androidx.workmanager)

            implementation(libs.androidx.documentfile)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.core.splashscreen)

            implementation(libs.work.runtime.ktx)
            implementation(libs.ui.tooling)
        }

        commonMain.dependencies {
            // api: the androidApp entry module compiles against these through this library
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.material.icons.extended)
            api(libs.compose.ui)
            api(libs.compose.components.resources)
            api(libs.ui.tooling.preview)
            implementation(libs.devsrsouza.compose.icons.eva)
            implementation(libs.navigation.compose)
            implementation(libs.compottie)
            implementation(libs.compottie.dot)
            implementation(libs.ui.backhandler)
            implementation(libs.mikepenz.aboutlibraries.core)
            implementation(libs.mikepenz.aboutlibraries.compose)
            api(libs.koin.core)
            api(libs.koin.compose)
            api(libs.koin.compose.viewmodel)
            api(libs.coroutines.core)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.paging)
            implementation(libs.androidx.datastore.preferences.core)
            api(libs.okio)
            api(libs.kotlinx.serialization)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.datetime.names)
            api(libs.androidx.lifecycle.viewmodel)
            api(libs.touchlab.kermit)
            implementation(libs.vico.compose)
            implementation(libs.vico.compose.m3)
            implementation(libs.androidx.paging.runtime)
            implementation(libs.androidx.paging.compose)
        }

        commonTest.dependencies {
            implementation(libs.bundles.shared.commonTest)
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.bundles.shared.androidTest)
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

compose.resources {
    // the androidApp entry module references generated resource accessors
    publicResClass = true
    packageOfResClass = "mytime_productivity.shared.generated.resources"
}

aboutLibraries {
    collect.configPath = file("config")
    export {
        outputFile = file("src/commonMain/composeResources/files/aboutlibraries.json")
        prettyPrint = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

// MigrationTestHelper loads exported schemas from the test assets; stage them into the
// host-test assets dir (git-ignored) so the Room migration tests run under Robolectric on
// every :shared:testAndroidHostTest.
val stageRoomSchemasForHostTest =
    tasks.register<Copy>("stageRoomSchemasForHostTest") {
        from("$projectDir/schemas")
        into("$projectDir/src/androidHostTest/assets")
    }
// Every task that consumes the androidHostTest assets dir (asset merge + lint analyze/model)
// must run after the schemas are staged into it.
tasks
    .matching {
        it.name == "mergeAndroidHostTestAssets" ||
            (it.name.contains("AndroidHostTest") && it.name.contains("lint", ignoreCase = true))
    }.configureEach {
        dependsOn(stageRoomSchemasForHostTest)
    }

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
}

// aboutlibraries.json is gitignored, so it must be (re)generated before the copy task
// that stages composeResources/files into the packaged resources
tasks.matching { it.name == "preBuild" || it.name == "copyNonXmlValueResourcesForCommonMain" }.configureEach {
    dependsOn("exportLibraryDefinitions")
}
