plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release signing is entirely environment-driven (set by release.yml from
// repo secrets). When the env vars are absent — every debug build — the
// release build type simply stays unsigned and nothing here evaluates.
val ksFile: String? = System.getenv("KEYSTORE_FILE")

android {
    namespace = "com.resonantsystems.pingthing"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.resonantsystems.pingthing"
        minSdk = 26
        targetSdk = 35
        // versionCode comes from CI run number — strictly increasing, zero bookkeeping
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        // versionName comes from the release tag (v9.3.0 → 9.3.0); fallback for debug
        versionName = System.getenv("VERSION_NAME") ?: "9.3.0"
    }

    signingConfigs {
        if (ksFile != null) {
            create("release") {
                storeFile = file(ksFile)
                storePassword = System.getenv("KEYSTORE_PASS")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASS")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (ksFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.webkit:webkit:1.11.0")
}

// Single source of truth: the instrument lives at repo-root/web/ping-thing.html.
// It is copied into assets/ at build time and never committed there.
val copyInstrument by tasks.registering(Copy::class) {
    from(rootProject.file("../web/ping-thing.html"))
    into(layout.projectDirectory.dir("src/main/assets"))
}

tasks.named("preBuild") {
    dependsOn(copyInstrument)
}
