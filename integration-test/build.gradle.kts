plugins {
    kotlin("jvm") version "1.3.72"
    id("com.lightbend.akka.grpc.gradle") version "0.8.4"
    idea
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.testcontainers:testcontainers:1.12.5")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// TODO Remove this workaround when https://github.com/akka/akka-grpc/issues/786 is fixed
tasks.named("printProtocLogs") {
    doFirst {
        val logFile = file(buildDir.toPath().resolve("akka-grpc-gradle-plugin.log"))
        if (!logFile.exists()) {
            mkdir("$buildDir")
            logFile.writeText("")
        }
    }
}

akkaGrpc {
    language = "Java"
    generateClient = true
    generateServer = false
}
