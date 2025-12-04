import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.lumo)
}

android {
    namespace = "com.bluemix.clients_lead"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bluemix.clients_lead"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Secrets
        val secretsFile = rootProject.file("local.properties")
        val secrets = Properties().apply { if (secretsFile.exists()) load(secretsFile.inputStream()) }

        val supabaseUrl = secrets.getProperty("SUPABASE_URL")
            ?: System.getenv("SUPABASE_URL")
            ?: ""
        val supabaseKey = secrets.getProperty("SUPABASE_ANON_KEY")
            ?: System.getenv("SUPABASE_ANON_KEY")
            ?: ""

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseKey\"")
    }

    buildTypes {
        debug {
            // optionally relax network if you really need http during dev:
            // manifestPlaceholders["usesCleartext"] = "true"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Fail fast if secrets are missing in CI
            buildConfigField(
                "boolean",
                "RELEASE_SECRETS_OK",
                "(\"${System.getenv("SUPABASE_URL") ?: ""}\".length > 0 && " +
                        "\"${System.getenv("SUPABASE_ANON_KEY") ?: ""}\".length > 0).toString()"
            )
        }
    }

    // Java/Kotlin toolchain
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    // (Alternatively) kotlin {
    //    jvmToolchain(17)
    // }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            // avoid META-INF collisions from Ktor/Coroutines/Okio/etc.
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*"
            )
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM + core set
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose.core)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    val ktorVersion = "2.3.7"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")

    // Ktor 3.x stack for Supabase
    implementation(libs.bundles.supabase.stack)

    // KotlinX
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Data & Work & Room (KSP)
    implementation(libs.androidx.datastore)
    implementation(libs.work.runtime)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    // Google Services (pull via catalog)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    // Accompanist
    implementation(libs.accompanist.permissions)

    // DI
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // Misc
    implementation(libs.timber)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
