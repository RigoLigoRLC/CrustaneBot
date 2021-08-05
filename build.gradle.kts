import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
}

group = "org.tech4c57"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    // KotlinX Coroutine
    api("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.5.0")
    api("org.mongodb", "mongodb-driver", "3.12.9")
    api("org.junit.jupiter:junit-jupiter-api:5.3.1")

    // Mirai
    val miraiVersion = "2.6.6"
    api("net.mamoe", "mirai-core-api", miraiVersion)     // For compilation
    runtimeOnly("net.mamoe", "mirai-core", miraiVersion) // For runtime
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.register<Jar>("crustane-jar") {
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)

    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })

    exclude(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.md"
    )

    archiveFileName.set("bot-$version.jar")
    manifest {
        attributes["Main-Class"] = "org.tech4c57.bot.MainKt"
    }
}