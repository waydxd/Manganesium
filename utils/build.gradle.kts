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
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}