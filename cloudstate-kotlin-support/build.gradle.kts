import com.google.protobuf.gradle.*

plugins {
    kotlin("jvm") version "1.3.72"
    id("com.google.protobuf") version "0.8.12"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.5"
    idea
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    api("io.cloudstate:cloudstate-java-support:0.4.7")
    implementation("net.bytebuddy:byte-buddy:1.10.7")
    implementation("net.bytebuddy:byte-buddy-agent:1.10.7")
    implementation("org.slf4j:slf4j-api:1.7.25")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.testcontainers:testcontainers:1.12.5")
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
        artifact = "com.google.protobuf:protoc:3.11.4"
    }
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            artifact(javadocJar.get())
            artifact(sourcesJar.get())

            pom {
                name.set("Cloudstate Kotlin")
                description.set("Cloudstate Kotlin Support Library")
                url.set("https://cloudstate.io/docs/user/lang/kotlin/index.html")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("cloudstate-contributors")
                        name.set("Cloudstate Contributors")
                        email.set("contributors@cloudstate.io")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/cloudstateio/kotlin-support.git")
                    developerConnection.set("scm:git:ssh://github.com/cloudstateio/kotlin-support.git")
                    url.set("https://github.com/cloudstateio/kotlin-support")
                }
                developers {
                    developer {
                        name.set("Adriano Santos")
                    }
                }
            }
        }
    }
}

val gitVersion: groovy.lang.Closure<String> by extra
val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")
    setPublications("maven")
    publish = true
    with(pkg) {
        userOrg = "cloudstateio"
        name = "cloudstate-kotlin-support"
        repo = if (versionDetails().isCleanTag) "releases" else "snapshots"
        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/cloudstatio/kotlin-support"
        with(version) {
            name = gitVersion()
            with(mavenCentralSync) {
                sync = versionDetails().isCleanTag
                user = System.getenv("SONATYPE_USER")
                password = System.getenv("SONATYPE_PASSWORD")
            }
        }
    }
}
