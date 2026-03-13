package io.github.lmliam.microsmith.cli.init

import java.nio.file.Path

internal val JVM_LANGUAGE_BUILD_MARKERS = listOf(
    Path.of("pom.xml"),
    Path.of("build.gradle"),
    Path.of("build.gradle.kts"),
    Path.of("settings.gradle"),
    Path.of("settings.gradle.kts"),
)

internal val SCALA_BUILD_MARKERS = listOf(
    Path.of("build.sbt"),
    Path.of("project/build.properties"),
) + JVM_LANGUAGE_BUILD_MARKERS
