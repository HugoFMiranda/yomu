plugins {
    id(Plugins.kotlinter.name) version Plugins.kotlinter.version
    id(Plugins.gradleVersions.name) version Plugins.gradleVersions.version
    id(Plugins.jetbrainsKotlin) version AndroidVersions.kotlin apply false
    id("org.jetbrains.kotlin.plugin.compose") version AndroidVersions.kotlin apply false
}
allprojects {
    repositories {
        // Vendored copies of JitPack artifacts JitPack can no longer build
        // (their own build scripts depend on JCenter, shut down permanently
        // in 2022): com.github.florent37:viewtooltip, br.com.simplepass:
        // loading-button-android, com.dmitrymalkovich.android:material-design-dimens,
        // com.github.inorichi.injekt:injekt-core. See local-repo/README.md.
        maven { setUrl("$rootDir/local-repo") }
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://plugins.gradle.org/m2/") }
    }
}

subprojects {
    // Rule configuration lives in .editorconfig (kotlinter 4+ dropped the DSL)
    apply(plugin = Plugins.kotlinter.name)
}

buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.9.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${AndroidVersions.kotlin}")
        classpath("com.google.android.gms:oss-licenses-plugin:0.10.6")
        classpath("org.jetbrains.kotlin:kotlin-serialization:${AndroidVersions.kotlin}")
    }
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

tasks.named("dependencyUpdates", com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask::class.java).configure {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
    // optional parameters
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
