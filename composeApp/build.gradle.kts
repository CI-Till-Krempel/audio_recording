import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    val iosArm = iosArm64()
    val iosSimArm = iosSimulatorArm64()

    listOf(iosArm, iosSimArm).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }

        // K/N cinterop with ARAudioKit (Swift framework)
        iosTarget.compilations.getByName("main") {
            cinterops {
                val ARAudioKit by creating {
                    defFile(project.file("src/nativeInterop/cinterop/ARAudioKit.def"))

                    // Prefer in-repo XCFramework at iosApp/ARAudioKit.xcframework
                    val xcRoot = project.rootProject.file("iosApp/ARAudioKit.xcframework")
                    val isSimulator = iosTarget.name.contains("Simulator", ignoreCase = true)
                    if (xcRoot.exists()) {
                        // Try common slice names and pick the first that contains the framework
                        val candidates = if (isSimulator) listOf(
                            "ios-arm64_x86_64-simulator",
                            "ios-arm64-simulator",
                            "ios-x86_64-simulator"
                        ) else listOf(
                            "ios-arm64"
                        )
                        val found: java.io.File? = candidates
                            .map { project.file("${xcRoot.absolutePath}/$it") }
                            .firstOrNull { dir -> project.file("${dir.absolutePath}/ARAudioKit.framework").exists() }
                        if (found != null) {
                            val fwkDir = found.absolutePath
                            val headersDir = project.file("$fwkDir/ARAudioKit.framework/Headers").absolutePath
                            // For cinterop header discovery we need both -F (framework) and -I (headers)
                            compilerOpts("-F$fwkDir")
                            compilerOpts("-I$headersDir")
                            includeDirs(headersDir)
                        }
                    }

                    // Enable modules for ObjC headers
                    compilerOpts("-fmodules")
                }
            }
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "de.cologneintelligence.audio_recording"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "de.cologneintelligence.audio_recording"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

