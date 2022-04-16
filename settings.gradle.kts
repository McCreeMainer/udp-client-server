pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven(url= "https://kotlin.bintray.com/kotlinx")
    }
}

rootProject.name = "UDP-client-server"

include(":server")
include(":client")