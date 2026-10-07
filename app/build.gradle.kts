plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

// Signing comes from environment variables set by the GitHub workflow.
val keystorePath: String? = System.getenv("KEYSTORE_PATH")

android {
  namespace = "hcloud.browser"
  compileSdk = 35

  defaultConfig {
    applicationId = "hcloud.browser"
    minSdk = 29
    targetSdk = 35
    versionCode = (System.getenv("BUILD_NUMBER") ?: "1").toInt()
    versionName = "1.0.${System.getenv("BUILD_NUMBER") ?: "0"}"
  }

  signingConfigs {
    if (keystorePath != null) {
      create("release") {
        storeFile = file(keystorePath)
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
}
