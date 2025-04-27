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
    implementation("org.slf4j:slf4j-api:2.0.3") // SLF4J dependency
    implementation("ch.qos.logback:logback-classic:1.5.13")
    testImplementation("org.mockito:mockito-core:2.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.16.1")
}

tasks.test {
    useJUnitPlatform()
    val agentJar = configurations.testRuntimeClasspath.get()
        .firstOrNull { it.name.contains("byte-buddy-agent") }
        ?.absolutePath
    if (agentJar != null) {
        jvmArgs("-javaagent:$agentJar")
    }
}

kotlin {
    jvmToolchain(23)
}