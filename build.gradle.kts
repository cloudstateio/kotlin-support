plugins {
    id("com.palantir.git-version") version "0.12.3"
}

allprojects {
    apply(plugin = "com.palantir.git-version")

    group = "io.cloudstate"

    val gitVersion: groovy.lang.Closure<String> by extra
    version = gitVersion()

    configurations.all {
        resolutionStrategy {
            dependencySubstitution {
                substitute(module("io.cloudstate:cloudstate-kotlin-support"))
                        .with(project(":cloudstate-kotlin-support"))
            }
        }
    }
}
