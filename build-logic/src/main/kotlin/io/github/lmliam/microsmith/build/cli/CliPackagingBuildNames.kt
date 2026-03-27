package io.github.lmliam.microsmith.build.cli

import org.gradle.api.Project

internal object CliPackagingBuildNames {
    val BUNDLED_PLUGIN_PROJECT_PATHS = listOf(
        ":resolve-schemas",
        ":resolve-schemas-protobuf",
        ":resolve-schemas-protobuf-rpc",
        ":artifact",
        ":artifact-schemas-protobuf",
        ":artifact-schemas-protobuf-rpc",
        ":compile-schemas-protobuf",
        ":compile-schemas-protobuf-rpc",
        ":gen",
    )

    const val BUNDLED_PLUGIN_CATALOG_FORMAT_VERSION = 1
    const val BUNDLED_PLUGIN_CATALOG_FILE_NAME = "bundled-plugins.lock"
    const val BUNDLED_PLUGIN_CATALOG_JAR_PATH = "META-INF/microsmith/$BUNDLED_PLUGIN_CATALOG_FILE_NAME"
    const val SHADOW_JAR_BASE_NAME = "microsmith-cli"
    const val SHADOW_JAR_CLASSIFIER = "all"
    const val DIST_OUTPUT_DIRECTORY = "distributions"
    const val DIST_BUILD_DIRECTORY = "microsmith-cli-dist"
    const val RELEASE_ASSETS_DIRECTORY = "release-assets"

    fun bundledPluginCoordinate(project: Project): String =
        "${project.group}:${project.name}:${project.version}"

    fun shadowJarArchiveName(version: String): String = "$SHADOW_JAR_BASE_NAME-$version-$SHADOW_JAR_CLASSIFIER.jar"

    fun distRootName(version: String): String = "$SHADOW_JAR_BASE_NAME-$version"
}
