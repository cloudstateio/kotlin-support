import com.google.protobuf.gradle.*

plugins {
    kotlin("jvm") version "1.3.72"
    id("com.google.protobuf") version "0.8.12"
    id("com.google.cloud.tools.jib") version "2.2.0"
    idea
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.cloudstate:kotlin-support:0.5.1")
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
        image = "cloudstateio/samples-kotlin-chat"
        tags = setOf(project.version.toString())
    }
    container {
        mainClass = "io.cloudstate.examples.chat.presence.MainKt"
        jvmFlags = listOf("-XX:+UseG1GC", "-XX:+UseStringDeduplication")
        ports = listOf("8080")
    }
}
