plugins {
    kotlin("jvm") // No version, inherited from root
    id("com.github.johnrengelman.shadow") // No version, inherited from root
    application
}

group = "org.manganesium"
version = "1.0.0"

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
tasks.register<JavaExec>("runTest") {
    group = "application"
    description = "Runs a specific main class for testing purposes."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("app.TestMainKt")
    dependsOn("testClasses") // Ensure test classes are compiled
    workingDir = rootDir
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
    mainClass.set("app.AppMainKt")
}

tasks.jar {
    archiveBaseName.set("Manganesium")
    archiveVersion.set("1.0.0")
    manifest {
        attributes["Main-Class"] = "app.AppMainKt"
    }
    from(sourceSets["main"].output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.shadowJar {
    archiveBaseName.set("Manganesium")
    archiveVersion.set("1.0.0")
    archiveClassifier.set("") // No classifier to replace the regular JAR
    manifest {
        attributes["Main-Class"] = "app.AppMainKt"
    }
    mergeServiceFiles() // Merge service descriptor files
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named("startScripts") {
    dependsOn(tasks.shadowJar)
}
tasks.named("startShadowScripts") {
    dependsOn(tasks.jar)
}