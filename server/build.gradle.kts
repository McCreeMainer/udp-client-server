plugins {
    application
    id("kotlin-platform-jvm")
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.0-native-mt")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")
    implementation("io.ktor:ktor-network:1.6.8")
    testImplementation(kotlin("test"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

application {
    mainClass.set("server.MainKt")
}
