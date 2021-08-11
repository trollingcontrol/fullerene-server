import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
}

group = "me.omskd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val exposedVersion = "0.31.1"
val http4kVersion = "4.10.0.1"

dependencies {
    testImplementation(kotlin("test-junit"))

    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-dao:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-java-time:${exposedVersion}")

    implementation("org.http4k:http4k-core:${http4kVersion}")
    implementation("org.http4k:http4k-format-gson:${http4kVersion}")
    implementation("org.http4k:http4k-security-oauth:${http4kVersion}")

    implementation("org.apache.logging.log4j:log4j:2.14.1")
    implementation("org.slf4j:slf4j-api:1.7.5")
    implementation("org.slf4j:slf4j-log4j12:1.7.5")

    implementation("com.auth0:java-jwt:3.18.1")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}