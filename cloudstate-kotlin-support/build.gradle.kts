import com.google.protobuf.gradle.*

plugins {
    kotlin("jvm") version "1.3.72"
    id("com.google.protobuf") version "0.8.12"
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    api("io.cloudstate:cloudstate-java-support:0.4.3")
    implementation("net.bytebuddy:byte-buddy:1.10.7")
    implementation("net.bytebuddy:byte-buddy-agent:1.10.7")
    implementation("org.slf4j:slf4j-api:1.7.25")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.testcontainers:testcontainers:1.12.5")
    testImplementation("com.google.api.grpc:proto-google-common-protos:1.17.0")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
}

tasks {
    processResources {
        filesMatching("**/version.prop") {
            expand("version" to version)
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.9.0"
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
