plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

tasks.register<JavaExec>("runClient") {
    group = "application"
    description = "Run the test client"
    mainClass.set("TestClientKt")
    classpath = sourceSets["main"].runtimeClasspath
}

kotlin {
    jvmToolchain(24)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    implementation("org.bytedeco:javacv:1.5.10")
    implementation("org.bytedeco:opencv-platform:4.9.0-1.5.10")
}
