plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
/*
 * This file was generated by the Gradle 'init' task.
 *
 * The settings file is used to specify which projects to include in your build.
 * For more detailed information on multi-project builds, please refer to https://docs.gradle.org/8.13/userguide/multi_project_builds.html in the Gradle documentation.
 * This project uses @Incubating APIs which are subject to change.
 */

rootProject.name = "manganesium"
include("crawler")
include("indexer")
include("search")
include("utils")
include("app")
