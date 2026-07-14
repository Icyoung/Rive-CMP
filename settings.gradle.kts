pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "rive-cmp-build"
include(":library")
project(":library").name = "rive-cmp"
include(":sample")
include(":androidSample")
