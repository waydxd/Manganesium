plugins {
    kotlin("jvm") version "2.1.10"
    application
}

group = "org.manganesium.crawler"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":utils"))  // Add dependency to utils module
    implementation(project(":indexer"))
    testImplementation(kotlin("test"))
    implementation("org.jsoup:jsoup:1.19.1") // Jsoup dependencydependencies
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3") // Kotlin logging dependency
    implementation("org.slf4j:slf4j-api:2.0.3") // SLF4J dependency
    implementation("ch.qos.logback:logback-classic:1.4.14")
    testImplementation("org.mockito:mockito-core:2.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.1")
    // https://mvnrepository.com/artifact/org.mockito/mockito-junit-jupiter
    testImplementation("org.mockito:mockito-junit-jupiter:5.16.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
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
application {
    // Fully qualified name of the class containing the main function
    mainClass.set("org.manganesium.crawler.CrawlerMainKt")
}