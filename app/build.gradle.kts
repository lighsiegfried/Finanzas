import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// version semantica en un solo lugar: versionCode = major*10000 + minor*100 + patch
val versionMajor = 1
val versionMinor = 0
val versionPatch = 1

// carga las credenciales de firma desde un archivo externo que no se versiona
val keystorePropertiesFile = rootProject.file("keystore.properties")
val hasReleaseKeystore = keystorePropertiesFile.exists()
val keystoreProperties = Properties().apply {
    if (hasReleaseKeystore) FileInputStream(keystorePropertiesFile).use { load(it) }
}

android {
    namespace = "com.kratt.finanzas"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kratt.finanzas"
        minSdk = 26
        targetSdk = 36
        versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch
        versionName = "$versionMajor.$versionMinor.$versionPatch"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // excluye las herramientas manuales de la suite automatica de pruebas
        testInstrumentationRunnerArguments += mapOf("notAnnotation" to "com.kratt.finanzas.ManualDeviceTest")
    }

    signingConfigs {
        // solo se crea la firma de release cuando existe el archivo externo con las credenciales
        if (hasReleaseKeystore) {
            create("release") {
                val store = keystoreProperties.getProperty("storeFile")
                val storePass = keystoreProperties.getProperty("storePassword")
                val alias = keystoreProperties.getProperty("keyAlias")
                val keyPass = keystoreProperties.getProperty("keyPassword")
                // si el archivo existe pero esta incompleto, la compilacion falla con un mensaje claro
                if (store.isNullOrBlank() || storePass.isNullOrBlank() || alias.isNullOrBlank() || keyPass.isNullOrBlank()) {
                    throw GradleException(
                        "keystore.properties incompleto: define storeFile, storePassword, keyAlias y keyPassword",
                    )
                }
                storeFile = rootProject.file(store)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // r8 y encogido de recursos activos para la version final
            isMinifyEnabled = true
            isShrinkResources = true
            // nunca debe salir una version depurable
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
            // sin keystore externo la release sale sin firmar, pero igual compila
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
        }
        create("staging") {
            initWith(getByName("release"))
            // build de validacion: se comporta como release pero firmado con la clave de depuracion
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            applicationIdSuffix = ".staging"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // buildconfig.debug controla la proteccion de pantalla
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // bouncycastle es un jar multi-release y trae este manifiesto por duplicado
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    // exporta el esquema de room para futuras migraciones
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // clases de tamano de ventana para la navegacion adaptable, la version la maneja el bom
    implementation("androidx.compose.material3:material3-window-size-class")
    // set completo de iconos de material, la version la maneja el bom de compose
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher.android)
    implementation(libs.bouncycastle.bcprov)

    implementation(libs.androidx.biometric)
    // fragment moderno: biometric 1.1.0 arrastra uno viejo que rompe ActivityResult (requestCode 16 bits)
    implementation("androidx.fragment:fragment:1.8.5")
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.work.runtime.ktx)
    // graficas locales, apache-2.0, sin telemetria ni red
    implementation(libs.vico.compose.m3)
    // widgets de pantalla de inicio con glance, apache-2.0, sin red ni telemetria
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.work.testing)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
