allprojects {
    group = "io.cloudstate"
    version = "0.5.1"

    configurations.all {
        resolutionStrategy {
            dependencySubstitution {
                substitute(module("io.cloudstate:kotlin-support"))
                        .with(project(":cloudstate-kotlin-support"))
            }
        }
    }
}
