plugins {
    id("com.android.application") version "8.7.2" apply false // Use the highest version
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("com.android.library") version "8.7.2" apply false // Use matching version for library
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}
// You do not need the extra id("com.android.tools.build:gradle") line.