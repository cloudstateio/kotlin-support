plugins {
    kotlin("jvm") version "1.3.72"
    id("com.google.protobuf") version "0.8.12"
    id("com.google.cloud.tools.jib") version "2.2.0"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.cloudstate:cloudstate-kotlin-support:0.4.3")
    implementation("com.google.api.grpc:proto-google-common-protos:1.17.0")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
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
        image = "vkorenev/shopping-cart"
        tags = setOf(project.version.toString())
    }
    container {
        mainClass = "com.example.shoppingcart.MainKt"
        jvmFlags = listOf("-XX:+UseG1GC", "-XX:+UseStringDeduplication")
        ports = listOf("8080")
    }
}
