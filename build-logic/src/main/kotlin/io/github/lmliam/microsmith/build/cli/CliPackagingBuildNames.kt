package io.github.lmliam.microsmith.build.cli

import org.gradle.api.Project

internal object CliPackagingBuildNames {
    val BUNDLED_PLUGIN_PROJECT_PATHS = listOf(
        ":gen-schemas",
        ":gen-schemas-protobuf",
        ":gen-schemas-protobuf-rpc",
    )

    const val APPLICATION_MAIN_CLASS = "io.github.lmliam.microsmith.cli.CliKt"
    const val BUNDLED_PLUGIN_CATALOG_FORMAT_VERSION = 1
    const val BUNDLED_PLUGIN_CATALOG_FILE_NAME = "bundled-plugins.lock"
    const val BUNDLED_PLUGIN_CATALOG_JAR_PATH = "META-INF/microsmith/$BUNDLED_PLUGIN_CATALOG_FILE_NAME"
    const val GENERATED_CATALOG_DIRECTORY = "generated/microsmith"
    const val SHADOW_JAR_BASE_NAME = "microsmith-cli"
    const val SHADOW_JAR_CLASSIFIER = "all"
    const val DIST_OUTPUT_DIRECTORY = "distributions"
    const val DIST_BUILD_DIRECTORY = "microsmith-cli-dist"
    const val RELEASE_ASSETS_DIRECTORY = "release-assets"
    const val JAR_TASK_NAME = "jar"
    const val PROCESS_RESOURCES_TASK_NAME = "processResources"
    const val SHADOW_JAR_TASK_NAME = "shadowJar"
    const val CHECK_TASK_NAME = "check"

    fun bundledPluginCoordinate(project: Project): String =
        "${project.group}:${project.name}:${project.version}"

    fun shadowJarArchiveName(version: String): String = "$SHADOW_JAR_BASE_NAME-$version-$SHADOW_JAR_CLASSIFIER.jar"

    fun distRootName(version: String): String = "$SHADOW_JAR_BASE_NAME-$version"
}
