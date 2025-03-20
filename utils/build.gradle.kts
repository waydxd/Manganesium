plugins {
    kotlin("jvm") version "2.1.10"
}

group = "org.manganesium.utils"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.mapdb:mapdb:3.0.8") // MapDB dependency
    implementation("org.jsoup:jsoup:1.19.1") // Jsoup dependency
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3") // Kotlin logging dependency
    implementation("org.slf4j:slf4j-simple:2.0.3") // SLF4J dependency
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}