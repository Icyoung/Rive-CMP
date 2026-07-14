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

rootProject.name = "rivecmp"
include(":library")
project(":library").name = "rive-cmp"
include(":sample")
include(":androidSample")
