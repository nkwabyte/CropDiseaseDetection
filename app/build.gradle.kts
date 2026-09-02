import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream
import co.touchlab.skie.configuration.SuppressSkieWarning

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.skie)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

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
            binaryOption("bundleId", "com.nkwabyte.cropdiseasedetection")
        }
        iosTarget.compilations.getByName("main") {
            cinterops {
                val ExecuTorchBridge by creating {
                    defFile(project.file("src/nativeInterop/cinterop/ExecuTorchBridge.def"))
                    packageName("com.nkwabyte.cropdiseasedetection.bridge")
                    includeDirs("${rootProject.projectDir}/iosApp/iosApp")
                }
            }
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

    val versionPropsFile = rootProject.file("version.properties")
    val versionProps = Properties()
    if (versionPropsFile.exists()) {
        versionProps.load(FileInputStream(versionPropsFile))
    }
    val defaultVersionCode = versionProps.getProperty("VERSION_CODE", "1").toIntOrNull() ?: 1
    val defaultVersionName = versionProps.getProperty("VERSION_NAME", "1.0.0")

    val finalVersionCode = project.findProperty("appVersionCode")?.toString()?.toIntOrNull() ?: defaultVersionCode
    val finalVersionName = project.findProperty("appVersionName")?.toString() ?: defaultVersionName

    defaultConfig {
        applicationId = "com.nkwabyte.cropdiseasedetection"
        minSdk = 24
        targetSdk = 35
        versionCode = finalVersionCode
        versionName = finalVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePropsFile = rootProject.file("secrets/keystore.properties")
            val keystoreProps = Properties()
            if (keystorePropsFile.exists()) {
                keystoreProps.load(FileInputStream(keystorePropsFile))
            }

            val keystorePath = System.getenv("KEYSTORE_PATH")
                ?: keystoreProps.getProperty("KEYSTORE_PATH")
                ?: "secrets/release.keystore"
            val keystoreFile = rootProject.file(keystorePath)

            val storePass = System.getenv("KEYSTORE_PASSWORD")
                ?: keystoreProps.getProperty("KEYSTORE_PASSWORD")
            val alias = System.getenv("KEY_ALIAS")
                ?: keystoreProps.getProperty("KEY_ALIAS")
            val keyPass = System.getenv("KEY_PASSWORD")
                ?: keystoreProps.getProperty("KEY_PASSWORD")

            if (keystoreFile.exists() && !storePass.isNullOrBlank() && !alias.isNullOrBlank()) {
                storeFile = keystoreFile
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass ?: storePass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
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
        // Store the ExecuTorch models uncompressed so their asset length is readable
        // via openFd() — ObjectDetector uses it to detect a model change and refresh
        // the copy cached in filesDir.
        noCompress += "pte"
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

skie {
    features {
        group("com.nkwabyte.cropdiseasedetection.common.data.DiseaseInfo.description") {
            SuppressSkieWarning.NameCollision(true)
        }
        group("com.nkwabyte.cropdiseasedetection.common.data.DiseaseTranslation.description") {
            SuppressSkieWarning.NameCollision(true)
        }
        group("androidx.compose.ui.unit.Density.toSp") {
            SuppressSkieWarning.NameCollision(true)
        }
    }
}
