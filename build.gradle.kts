buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.11")
    }
}
plugins {
    id("com.android.application") version "9.4.0" apply false
    id("com.google.devtools.ksp") version "2.3.11" apply false
    id("com.google.dagger.hilt.android") version "2.60.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}
