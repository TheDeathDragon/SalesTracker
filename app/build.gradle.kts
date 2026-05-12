import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.lsplugin.resopt)
}

val branch: String = "NOTEAIR"
val projectName: String = "SalesTracker"
val apkFileName: String = "$projectName.apk"
val currentBuildTime: String = SimpleDateFormat("yy/MM/dd HH:mm:ss").format(Date())
val currentVersionDate: Int = SimpleDateFormat("yyMMdd").format(Date()).toInt()
val currentVersion: String = SimpleDateFormat("yy.MM.dd").format(Date())
val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
val date: String = dateFormat.format(Date())

android {
    namespace = "la.shiro.salestracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "la.shiro.salestracker"
        minSdk = 31
        targetSdk = 36
        versionCode = currentVersionDate
        versionName = currentVersion
        buildConfigField("long", "VERSION_CODE", "$currentVersionDate")
        buildConfigField("String", "BUILD_TIME", "\"$currentBuildTime\"")
        buildConfigField("String", "APP_NAME", "\"$projectName\"")
    }

    signingConfigs {
        getByName("debug") {
            keyAlias = "android"
            keyPassword = "android"
            storeFile = file("T.jks")
            storePassword = "android"
        }
        create("release") {
            keyAlias = "android"
            keyPassword = "android"
            storeFile = file("T.jks")
            storePassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            isShrinkResources = true
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.all {
        outputs.all {
            if (this is ApkVariantOutputImpl) {
                outputFileName = apkFileName
            }
        }
    }
}

allprojects {
    gradle.projectsEvaluated {
        tasks.register<Zip>("zipReleaseApkAndAssets") {
            val apkFile: java.io.File = file("release/$apkFileName")
            val outputDir: java.io.File = file("dist")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            from(apkFile) {
                into(projectName)
            }
            from("etc") {
                into(projectName)
            }
            archiveFileName.set("${projectName}_${branch}_${date}.zip")
            destinationDirectory.set(outputDir)
            doLast {
                println("ZIP file created at: ${outputDir.absolutePath}/${archiveFileName.get()}")
            }
        }
        tasks.register<Zip>("zipDebugSymbols") {
            val mappingFile: java.io.File = file("build/outputs/mapping/release/mapping.txt")
            val outputDir: java.io.File = file("dist")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            from(mappingFile) {
                into("DebugSymbols")
            }
            archiveFileName.set("${projectName}_${branch}_${date}_Symbols.zip")
            destinationDirectory.set(outputDir)
            doLast {
                println("Symbols file created at: ${outputDir.absolutePath}/${archiveFileName.get()}")
            }
        }
        tasks.getByName("assembleRelease").finalizedBy("zipReleaseApkAndAssets")
        tasks.getByName("zipReleaseApkAndAssets").finalizedBy("zipDebugSymbols")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(files("libs/nvram.jar"))
    compileOnly(files("libs/framework.jar"))
}
