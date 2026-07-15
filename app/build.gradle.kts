plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace  = "com.example.qcmfrance"
    compileSdk = 35

    defaultConfig {
        // Pas de préfixe com.example : refusé par le Play Store, et l'applicationId
        // ne peut plus changer après la première publication. Le namespace (packages
        // du code) reste com.example.qcmfrance — seul l'identifiant publié change.
        applicationId = "com.borisboiakki.qcmfrance"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 3
        versionName   = "0.5.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

ksp {
    // Export du schéma Room (exportSchema = true) : les JSON versionnés dans app/schemas
    // documentent chaque version de la BDD et permettent de tester les migrations.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation)

    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.compose)
    implementation(libs.coroutines.android)
    implementation(libs.gson)
    implementation(libs.datastore.preferences)

    debugImplementation(libs.compose.ui.tooling.debug)
}
