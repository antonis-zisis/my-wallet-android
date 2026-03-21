import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.apollo)
}

// Load local.properties for secrets
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "com.mywallet.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mywallet.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supabase credentials from local.properties
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties["supabase.url"] ?: ""}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"${localProperties["supabase.publishable_key"] ?: ""}\""
        )
        // GraphQL backend URL
        buildConfigField(
            "String",
            "GRAPHQL_URL",
            "\"${localProperties["graphql.url"] ?: "http://10.0.2.2:4000/graphql"}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

apollo {
    service("service") {
        packageName.set("com.mywallet.android.graphql")
        // Schema and .graphql files are auto-discovered from src/main/graphql/
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(libs.splashscreen)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Apollo Kotlin (GraphQL)
    implementation(libs.apollo.runtime)

    // OkHttp (Supabase REST auth calls)
    implementation(libs.okhttp)

    // Charts
    implementation(libs.vico.compose.m3)

    // DataStore
    implementation(libs.datastore.preferences)

    // Debug
    debugImplementation(libs.compose.ui.tooling)
}
