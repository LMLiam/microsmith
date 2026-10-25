package io.github.lmliam.microsmith.build.cli

import org.gradle.api.Project

internal object BundledPlugins {
    val PROJECT_PATHS = listOf(
        ":resolve-services",
        ":resolve-services-dotnet",
        ":resolve-services-dotnet-asp",
        ":resolve-services-dotnet-packages",
        ":resolve-schemas",
        ":resolve-schemas-protobuf",
        ":resolve-schemas-protobuf-rpc",
        ":artifact",
        ":artifact-services",
        ":artifact-services-dotnet",
        ":artifact-services-dotnet-asp",
        ":artifact-services-dotnet-packages",
        ":artifact-schemas-protobuf",
        ":artifact-schemas-protobuf-rpc",
        ":compile-services",
        ":compile-services-dotnet",
        ":compile-services-dotnet-asp",
        ":compile-services-dotnet-packages",
        ":compile-schemas-protobuf",
        ":compile-schemas-protobuf-rpc",
        ":gen",
    )

    fun coordinate(project: Project): String = "${project.group}:${project.name}:${project.version}"
}
