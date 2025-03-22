plugins {
    kotlin("jvm") version "2.1.10"
}

group = "org.manganesium"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":utils"))  // Add dependency to utils module
    implementation(project(":indexer"))  // Add dependency to indexer module
    implementation(project(":crawler"))  // Add dependency to crawler module
    testImplementation(kotlin("test"))

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}