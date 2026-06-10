plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.resonantsystems.pingthing"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.resonantsystems.pingthing"
        minSdk = 26
        targetSdk = 35
        // versionCode comes from CI run number — strictly increasing, zero bookkeeping
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "9.3.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
