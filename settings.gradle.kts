pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "IrisCode"

include(":app")

// Termux terminal emulator (vendored from github.com/termux/termux-app)
include(":termux:emulator")
include(":termux:view")