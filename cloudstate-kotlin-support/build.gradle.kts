plugins {
    kotlin("jvm") version "1.3.72"
    id("com.lightbend.akka.grpc.gradle") version "0.8.4"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("io.cloudstate:cloudstate-java-support:0.4.3")
    implementation("net.bytebuddy:byte-buddy:1.10.7")
    implementation("net.bytebuddy:byte-buddy-agent:1.10.7")
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("ch.qos.logback:logback-core:1.2.3")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.testcontainers:testcontainers:1.12.5")
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
    serverPowerApis = true
}
