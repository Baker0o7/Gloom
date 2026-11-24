plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.aboutlibraries)
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.addAll("-Xcontext-parameters", "-Xexpect-actual-classes")
    }
}

android {
    compileSdk = 35
    namespace = "dev.materii.gloom"

    defaultConfig {
        applicationId = "dev.materii.gloom"
        minSdk = 21
        targetSdk = 35
        // CI passes the release tag (e.g. v1.0.90) so the APK's version matches the release.
        val ciVersion = System.getenv("APP_VERSION")?.removePrefix("v")?.takeIf { it.isNotBlank() }
        val parts = ciVersion?.split(".")?.mapNotNull { it.toIntOrNull() }
        versionCode = if (parts != null && parts.size == 3) parts[0] * 10000 + parts[1] * 100 + parts[2] else 10090
        versionName = ciVersion ?: "1.0.90"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Shared debug key (public, debug-only) so every CI-built APK has the same signature
        // and can be installed over the previous one instead of requiring an uninstall.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            storeFile = System.getenv("SIGNING_STORE_FILE")?.let(::File)
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
        }
    }

    buildTypes {
        release {
            val hasReleaseSigning = System.getenv("SIGNING_STORE_PASSWORD")?.isNotEmpty() == true

            // versionNameSuffix = "-alpha" // TODO: Use build flavor
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            signingConfig = signingConfigs.getByName(if (hasReleaseSigning) "release" else "debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "../proguard-rules.pro")
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    applicationVariants.all {
        val variant = this
        outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val outputFileName = "gloom-${variant.buildType.name}.apk"
                output.outputFileName = outputFileName
            }
    }

    androidResources {
        generateLocaleConfig = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(project.layout.projectDirectory.file("../stability.txt"))
}

dependencies {
    implementation(libs.bundles.kotlinx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.glance.appwidget)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.glance.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.koin.android)
    implementation(libs.voyager.navigator)

    implementation(project(":api"))
    implementation(project(":shared"))
    implementation(project(":ui"))
}
