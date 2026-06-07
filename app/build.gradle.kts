import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.material)
            implementation(libs.androidx.activity.compose)
            
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.koin.android)

            // Firebase BOM for GitLive
            implementation(project.dependencies.platform(libs.firebase.bom))

            // ML Dependencies
            implementation(libs.executorch.android)
            implementation(libs.pytorch.android)
            implementation(libs.pytorch.android.torchvision)

            // Auth Dependencies for Android
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)

            implementation(libs.ktor.client.okhttp)
        }
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(compose.materialIconsExtended)

            // KMP Navigation
            implementation(libs.nav.compose)

            // KMP Serialization
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)

            // KMP Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // KMP Firebase
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.auth)

            // KMP Coil
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // KMP Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // KMP Crypto
            implementation(libs.kotlincrypto.hash.sha1)

            // Firebase
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.auth)

            // KMP Window size class
            implementation(libs.androidx.compose.material3.windowsizeclass)
            
            // Lottie - we may need KMP version or omit
            // implementation(libs.lottie.compose) // Warning: Lottie is Android only by default. Needs compottie for KMP.
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.nkwabyte.cropdiseasedetection"
    compileSdk = 35

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")
    sourceSets["main"].assets.srcDirs("src/androidMain/assets", "src/commonMain/assets")

    defaultConfig {
        applicationId = "com.nkwabyte.cropdiseasedetection"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    androidResources {
        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:.*:!CVS:!thumbs.db:!picasa.ini:!*~"
    }

    packaging {
        jniLibs {
            pickFirsts.add("**/libc++_shared.so")
            pickFirsts.add("**/libfbjni.so")
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
configurations.all {
    exclude(group = "com.facebook.fbjni", module = "fbjni-java-only")
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.nkwabyte.cropdiseasedetection.generated.resources"
}

buildkonfig {
    packageName = "com.nkwabyte.cropdiseasedetection"
    val credsFile = file("../local.properties")
    val credsProps = Properties()
    if (credsFile.exists()) {
        credsProps.load(FileInputStream(credsFile))
    }

    defaultConfigs {
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "CLOUDINARY_API_KEY", credsProps.getProperty("CLOUDINARY_API_KEY", ""))
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "CLOUDINARY_API_SECRET", credsProps.getProperty("CLOUDINARY_API_SECRET", ""))
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "CLOUDINARY_CLOUD_NAME", "dxdun6eym")
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "GOOGLE_WEB_CLIENT_ID", credsProps.getProperty("GOOGLE_WEB_CLIENT_ID", ""))
    }
}
