// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    plugins.withId("dev.detekt") {
        extensions.configure<dev.detekt.gradle.extensions.DetektExtension> {
            config.setFrom(rootProject.file("config/detekt/detekt.yml"))
            buildUponDefaultConfig.set(true)
            parallel.set(true)
        }
    }
}

val checkArchitecture =
    tasks.register("checkArchitecture") {
        group = "verification"
        description = "Checks that module dependency boundaries are not violated by imports."

        doLast {
            val forbiddenImports =
                mapOf(
                    "domain/src/main" to
                        listOf(
                            "android.",
                            "androidx.",
                            "com.m4isper.contactsapp.data.",
                            "com.m4isper.contactsapp.presentation.",
                            "com.m4isper.contactsapp.di.",
                            "dev.zacsweers.metro.",
                            "dev.zacsweers.metrox.",
                        ),
                    "data/src/main" to
                        listOf(
                            "com.m4isper.contactsapp.presentation.",
                            "com.m4isper.contactsapp.di.",
                        ),
                    "app/src/main/java/com/m4isper/contactsapp/presentation" to
                        listOf(
                            "com.m4isper.contactsapp.data.",
                        ),
                )
            val violations = mutableListOf<String>()
            forbiddenImports.forEach { (sourceRoot, prefixes) ->
                val root = rootProject.file(sourceRoot)
                if (!root.exists()) return@forEach
                root.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .forEach { file ->
                        file.useLines { lines ->
                            lines.forEachIndexed { index, line ->
                                val imported = line.trim().removePrefix("import ").trim()
                                if (line.trim().startsWith("import ") && prefixes.any(imported::startsWith)) {
                                    violations += "${file.relativeTo(rootDir)}:${index + 1}: $imported"
                                }
                            }
                        }
                    }
            }
            if (violations.isNotEmpty()) {
                throw GradleException(
                    "Architecture violations:\n${violations.joinToString("\n")}",
                )
            }
        }
    }

subprojects {
    tasks.configureEach {
        if (name == "check") dependsOn(checkArchitecture)
    }
}
