import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    // Kotlin 1.9.25 matches the Kotlin runtime bundled with the target IDE
    // (2024.1 / build 241), which avoids stdlib version clashes at runtime.
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    // The modern (2.x) IntelliJ Platform Gradle Plugin.
    // Pinned to 2.1.0: it is verified against this project's Gradle 8.10.2
    // wrapper and Kotlin 1.9.25. Plugin 2.6.0+ requires Gradle 9, which is not
    // yet safe with Kotlin 1.9.x — bump all three together when upgrading.
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    // Repositories that host the IntelliJ Platform artifacts and tooling.
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Build against IntelliJ IDEA Community. Everything this plugin uses
        // (ProjectTaskListener, the SM test runner, the Kotlin UI DSL,
        // PersistentStateComponent) is part of the shared platform, so the
        // resulting plugin also runs unchanged on Android Studio.
        intellijIdeaCommunity(providers.gradleProperty("platformVersion").get())

        // Marketplace tooling. These are resolved lazily and only used by the
        // verifyPlugin / signPlugin / publishPlugin tasks — they do not slow
        // down a plain `buildPlugin`.
        pluginVerifier()
        zipSigner()

        // Lightweight platform test fixtures (kept for completeness; the unit
        // tests in this project are plain JUnit 5 and do not start the IDE).
        testFramework(TestFrameworkType.Platform)
    }

    // --- Bundled runtime libraries ---------------------------------------
    // WAV is handled natively by the JDK. These add MP3 decoding. They are
    // packaged into the plugin's lib/ folder by the build. The `junit`
    // exclusion stops these legacy POMs from dragging JUnit 3.8.2 into the
    // shipped plugin.
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4") { exclude(group = "junit") }
    implementation("com.googlecode.soundlibs:jlayer:1.0.1.4") { exclude(group = "junit") }
    implementation("com.googlecode.soundlibs:tritonus-share:0.3.7.4") { exclude(group = "junit") }

    // --- Tests (JUnit 5) --------------------------------------------------
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // The IntelliJ platform test framework registers a JUnit Platform session
    // listener that is built on JUnit 4. JUnit 4 must therefore be on the test
    // runtime classpath even though our own tests are written with JUnit 5.
    testRuntimeOnly("junit:junit:4.13.2")
}

intellijPlatform {
    // This is a pure-Kotlin plugin with no Java sources or Swing *.form files,
    // so there is nothing to instrument. Disabling it removes the need for the
    // `instrumentationTools()` dependency and speeds up the build.
    instrumentCode = false

    // The plugin's single settings page contributes no statically-indexable
    // searchable options, so skip the (slow) headless indexing step.
    buildSearchableOptions = false

    pluginConfiguration {
        // name / vendor / description / change-notes live in plugin.xml.
        // Here we only patch the values that are environment-specific.
        version = providers.gradleProperty("pluginVersion")
        ideaVersion {
            sinceBuild = "241"
            // No upper bound. The plugin relies only on long-stable platform
            // APIs (verified compatible against 241 & 243), so it should load in
            // current and future IDEs — including Android Studio 2025.3 "Panda"
            // (platform build 253) and later. Re-introduce a bound only if a
            // future release is found to break compatibility.
            untilBuild = provider { null }
        }
    }

    // `./gradlew signPlugin` and `publishPlugin` read these from the
    // environment so secrets never live in version control.
    signing {
        certificateChainFile = file(System.getenv("CERTIFICATE_CHAIN") ?: "certificate/chain.crt")
        privateKeyFile = file(System.getenv("PRIVATE_KEY") ?: "certificate/private.pem")
        password = System.getenv("PRIVATE_KEY_PASSWORD")
    }
    publishing {
        token = System.getenv("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            // Verify against the build-target IDE (compatibility floor) and a
            // newer release to validate the declared since/until-build range.
            // Swap in `recommended()` for an exhaustive pre-release sweep.
            ide("IC", providers.gradleProperty("platformVersion").get()) // 2024.1.7 (floor)
            ide("IC", "2024.3")                                          // forward-compat check
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        // Keep the language level aligned with the bundled Kotlin runtime.
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}
