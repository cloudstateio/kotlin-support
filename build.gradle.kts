allprojects {
    group = "io.cloudstate"
    version = "0.4.3"

    configurations.all {
        resolutionStrategy {
            dependencySubstitution {
                substitute(module("io.cloudstate:cloudstate-kotlin-support"))
                        .with(project(":cloudstate-kotlin-support"))
            }
        }
    }
}
