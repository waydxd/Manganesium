plugins {
    kotlin("jvm") // No version, inherited from root
    kotlin("plugin.serialization") version "2.1.10"
}

group = "org.manganesium"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":utils"))  // Add dependency to utils module
    
    // MapDB dependency (same as utils module)
    implementation("org.mapdb:mapdb:3.0.8")
    
    // Ktor client dependencies for OpenAI API
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7") 
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-jackson:2.3.7")
    implementation("io.ktor:ktor-client-logging:2.3.7")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Logging
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.ktor:ktor-client-mock:2.3.7")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}