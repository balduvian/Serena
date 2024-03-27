plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.0.0"
    kotlin("jvm") version "1.9.22"
    application
}

group = "org.balduvian"
version = "0.1"

repositories {
    mavenCentral()
    maven {
        name="m2-dv8tion"
        url=uri("https://m2.dv8tion.net/releases")
    }
}

val ktorVersion: String by project

dependencies {
    implementation ("net.dv8tion:JDA:5.0.0-beta.20") {
        exclude(module="opus-java")
    }
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "BotMainKt"
    }

    archiveFileName.set("serena.jar")
}

tasks.register<Copy>("copyProduction") {
    from("./build/libs/Serena.jar")
    into("./production")
    dependsOn(tasks.shadowJar)
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("BotMainKt")
    tasks.run.get().workingDir("./development")
}