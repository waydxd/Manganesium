plugins {
    kotlin("jvm") version "2.1.10"
    application
}

group = "org.manganesium"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    val ktorVersion = "3.1.2"

    implementation(project(":utils"))  // Add dependency to utils module
    implementation(project(":indexer"))  // Add dependency to indexer module
    implementation(project(":crawler"))  // Add dependency to crawler module
    implementation(project(":search"))
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    testImplementation(kotlin("test"))

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}
tasks.named<JavaExec>("run") {
    workingDir = rootDir
}
application {
    // Fully qualified name of the class containing the main function
    mainClass.set("app.TestMainKt")
}