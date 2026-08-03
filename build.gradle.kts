// Top-level build file. Individual module build files apply plugins from here via version catalogs-free direct versions
// (kept simple/explicit on purpose — no version catalog — so a mobile-only workflow stays easy to read/edit on GitHub's web editor).
plugins {
    id("com.android.application") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
