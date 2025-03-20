plugins {
    kotlin("jvm") version "2.1.10"
}

group = "org.manganesium.search"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":utils"))  // Add dependency to utils module
    testImplementation(kotlin("test"))
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3") // Kotlin logging dependency
    implementation("org.slf4j:slf4j-simple:2.0.3") // SLF4J dependency

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}