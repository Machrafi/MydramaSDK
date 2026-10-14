pluginManagement {
    repositories {
        google()
        // Google-hosted Maven Central mirror avoids shared CI download throttling.
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Google-hosted Maven Central mirror avoids shared CI download throttling.
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
    }
}

rootProject.name = "mydrama-android-sdk"
include(":mydrama-sdk")
