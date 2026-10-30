plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
}

// Имя приложения в именах собранных файлов — константой: чтение rootProject.name
// из скрипта модуля мешает configuration cache.
val appFileName = "ContactsApp"

// versionName ведётся руками, versionCode — число коммитов: растёт сам, ручной
// бамп не нужен. Rebase или squash уменьшают счётчик, и следующий билд получит
// меньший versionCode, чем уже выпущенный, — после переписывания истории
// проверить счётчик перед релизом.
val appVersionName = "1.0.7"

fun git(vararg args: String): String =
    providers
        .exec {
            workingDir(rootDir)
            commandLine("git", *args)
        }.standardOutput.asText
            .get()
            .trim()

val commitCount = git("rev-list", "--count", "HEAD").toInt()
val commitHash = git("rev-parse", "--short", "HEAD")

android {
    namespace = "com.m4isper.contactsapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.m4isper.contactsapp"
        minSdk = 26
        targetSdk = 37
        versionCode = commitCount
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableAndroidTestCoverage = true
            // Суффикс виден в настройках телефона: сразу ясно, что стоит не
            // релиз, и виден коммит этой сборки.
            versionNameSuffix = "-dev-$commitHash"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Стенды android.util.Log и подобных классов в JVM-тестах не должны падать.
            isReturnDefaultValues = true
            // Robolectric-тестам обёртки языка нужны ресурсы приложения.
            isIncludeAndroidResources = true
        }
    }

    lint {
        // Паритет локалей обязателен: пропущенный перевод ломает локаль молча.
        error += "MissingTranslation"
    }

    // Язык переключается внутри приложения, поэтому локали не должны
    // вырезаться из базового APK в language-splits бандла.
    bundle {
        language {
            enableSplit = false
        }
    }
}

// Имя APK: ContactsApp-debug-1.0.7-252-208a967.apk — вариант, версия и коммит
// видны без открывания build/outputs. На имя .aab в Play это не влияет.
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set(
                "$appFileName-${variant.name}-$appVersionName-$commitCount-$commitHash.apk",
            )
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.glance.appwidget.preview)
    implementation(libs.androidx.glance.preview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.icons.core)
    implementation(libs.androidx.icons.extended)
    implementation(libs.metrox.viewmodel)
    implementation(libs.metrox.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.coil.compose)
    implementation(libs.zxing.core)
    implementation(libs.zxing.embedded)
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.adaptive.layout)
    implementation(libs.androidx.adaptive.navigation3)
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.robolectric)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.vintage)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
