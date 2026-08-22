plugins {
      id("com.android.application")
      kotlin("android")
  }

  android {
      compileSdk = 36
      namespace = "com.mkdev.mkkeyboard"
      buildFeatures {
          buildConfig = true
      }

      defaultConfig {
          applicationId = "com.mkdev.mkkeyboard"
          minSdk = 24
          targetSdk = 36
          versionCode = 1
          versionName = "1.0"
        buildConfigField(
            "String",
            "GIPHY_API_KEY",
            "\"${System.getenv("GIPHY_API_KEY") ?: "9I9AcTIdNuIIVOBptqTEkeWkNU2gII8D"}\""
        )
      }

      signingConfigs {
          create("release") {
              storeFile     = file("release.keystore")
              storePassword = System.getenv("STORE_PASSWORD") ?: ""
              keyAlias      = System.getenv("KEY_ALIAS") ?: ""
              keyPassword   = System.getenv("KEY_PASSWORD") ?: ""
              storeType     = "PKCS12"
          }
      }

      buildTypes {
          release {
              isMinifyEnabled = false
              signingConfig   = signingConfigs.getByName("release")
          }
      }

      compileOptions {
          sourceCompatibility = JavaVersion.VERSION_11
          targetCompatibility = JavaVersion.VERSION_11
      }

      kotlinOptions {
          jvmTarget = "11"
      }
  }

  dependencies {
      implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
      implementation("androidx.core:core-ktx:1.12.0")
      implementation("androidx.appcompat:appcompat:1.6.1")
  }