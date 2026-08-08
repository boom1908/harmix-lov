plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20" apply false
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
    id("com.chaquo.python") version "16.0.0" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://chaquo.com/maven") }
        maven { url = uri("https://jitpack.io") }
    }
}
