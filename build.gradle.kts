plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "com.birtdata"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.edgedb:driver:0.2.1")
    implementation("org.apache.kafka:kafka-clients:2.7.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}