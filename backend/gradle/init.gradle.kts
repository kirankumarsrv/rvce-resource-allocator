// Configure Java Toolchain Download Repositories
gradle.settingsEvaluated {
    settings.java.toolchain {
        downloadRepositories {
            repository("https://api.adoptopenjdk.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/adoptopenjdk") {
                resolveStrategy = org.gradle.java.toolchain.JavaToolchainRepositoryResolveStrategy.PREFER
            }
        }
    }
}
