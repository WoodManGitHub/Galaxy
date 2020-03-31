import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    //    "maven-publish"
    kotlin("jvm") version "1.3.61"
    id("fabric-loom") version "0.2.6-SNAPSHOT"
}

val version = "0.0.1"
val group = "one.oktw"

val fabricVersion = "0.1.2+b7f9825d30"
val proxyApiVersion = "0.1.0"

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://jitpack.io")
}

base {
    archivesBaseName = "Galaxy"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        apiVersion = "1.3"
        languageVersion = "1.3"
        jvmTarget = "1.8"
    }
}

minecraft {
}

dependencies {
    // Core
    minecraft(group = "com.mojang", name = "minecraft", version = "20w13b")
    mappings(group = "net.fabricmc", name = "yarn", version = "20w13b+build.9", classifier = "v2")
    modImplementation(group = "net.fabricmc", name = "fabric-loader", version = "0.7.8+build.189")

    // fabric api/library
    modImplementation(group = "net.fabricmc", name = "fabric-language-kotlin", version = "1.3.61+build.2")
    modImplementation(group = "net.fabricmc.fabric-api", name = "fabric-api-base", version = fabricVersion)
    modImplementation(group = "net.fabricmc.fabric-api", name = "fabric-commands-v0", version = fabricVersion)
    modImplementation(group = "net.fabricmc.fabric-api", name = "fabric-events-lifecycle-v0", version = fabricVersion)

    // galaxy api
    modImplementation(group = "one.oktw", name = "galaxy-proxy", version = proxyApiVersion)

    // Jar in Jar
    include(group = "net.fabricmc.fabric-api", name = "fabric-api-base", version = fabricVersion)
    include(group = "net.fabricmc.fabric-api", name = "fabric-commands-v0", version = fabricVersion)
    include(group = "net.fabricmc.fabric-api", name = "fabric-events-lifecycle-v0", version = fabricVersion)
    include(group = "one.oktw", name = "galaxy-proxy", version = proxyApiVersion)
    include(group = "org.mongodb", name = "bson", version = "4.0.1")
}

tasks.getByName<ProcessResources>("processResources") {
    inputs.property("version", version)

    from(sourceSets.getByName("main").resources.srcDirs) {
        include("fabric.mod.json")
        expand(Pair("version", version))
    }

    from(sourceSets.getByName("main").resources.srcDirs) {
        exclude("fabric.mod.json")
    }
}

tasks.getByName<Jar>("jar") {
    from("LICENSE")
}
