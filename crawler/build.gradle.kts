plugins {
    kotlin("jvm") version "2.1.10"
}

group = "org.manganesium.crawler"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":utils"))  // Add dependency to utils module
    testImplementation(kotlin("test"))
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3") // Kotlin logging dependency
    implementation("org.jsoup:jsoup:1.19.1") // Jsoup dependency
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}