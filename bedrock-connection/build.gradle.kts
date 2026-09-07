java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

lombok {
    version = "1.18.42"
}

dependencies {
    api(projects.bedrockCodec)
    api(libs.netty.transport.raknet)
    api(libs.snappy)

    testImplementation(libs.junit)
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "org.cloudburstmc.protocol.bedrock.connection")
    }
}
