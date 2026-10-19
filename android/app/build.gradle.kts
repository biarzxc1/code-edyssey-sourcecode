plugins {
  alias(libs.plugins.android.application)
}

android {
  namespace = "com.odyssey.game"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.odyssey.game"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"
  }

  signingConfigs {
    create("release") {
      storeFile = file("release.keystore")
      storePassword = "odyssey2026"
      keyAlias = "odyssey"
      keyPassword = "odyssey2026"
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("release")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.core:core-splashscreen:1.0.1")
}
