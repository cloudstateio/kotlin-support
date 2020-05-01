import com.google.protobuf.gradle.*

plugins {
    kotlin("jvm") version "1.3.72"
    id("com.google.protobuf") version "0.8.12"
    id("com.google.cloud.tools.jib") version "2.2.0"
    idea
}

repositories {
    // This repository is needed to find cloudstate-kotlin-support installed to the local Maven repository
    // TODO remove local repository when cloudstate-kotlin-support is published to a public repository
    mavenLocal()
    mavenCentral()
    // This repository is needed for a snapshot version of akka-grpc which cloudstate-java-support depends on
    // TODO remove this repository when cloudstate-java-support upgrades its dependency
    maven {
        url = uri("https://dl.bintray.com/akka/maven")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.cloudstate:kotlin-support:0.4.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.11.4"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

jib {
    from {
        image = "openjdk:8u242-jdk"
    }
    to {
        image = "sleipnir/kotlin-chat"
        tags = setOf(project.version.toString())
    }
    container {
        mainClass = "io.cloudstate.examples.chat.presence.MainKt"
        jvmFlags = listOf("-XX:+UseG1GC", "-XX:+UseStringDeduplication")
        ports = listOf("8080")
    }
}
