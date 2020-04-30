allprojects {
    group = "io.cloudstate"
    version = "0.5.0"

    configurations.all {
        resolutionStrategy {
            dependencySubstitution {
                substitute(module("io.cloudstate:cloudstate-kotlin-support"))
                        .with(project(":cloudstate-kotlin-support"))
            }
        }
    }
}
