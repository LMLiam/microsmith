package io.github.lmliam.microsmith.build.runtime

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.Project

internal object RuntimeScriptingBuildNames {
    val API_PROJECT_PATHS = listOf(
        ":dsl",
        ":dsl-schemas",
        ":dsl-schemas-protobuf",
        ":dsl-schemas-protobuf-rpc",
    )

    val IMPLEMENTATION_PROJECT_PATHS = listOf(
        ":resolve",
        ":resolve-schemas",
        ":resolve-schemas-protobuf",
        ":resolve-schemas-protobuf-rpc",
        ":artifact",
        ":artifact-schemas",
        ":artifact-schemas-protobuf",
        ":artifact-schemas-protobuf-rpc",
        ":lower",
        ":lower-schemas-protobuf",
        ":lower-schemas-protobuf-rpc",
        ":gen",
        ":gen-schemas",
        ":gen-schemas-protobuf",
        ":gen-schemas-protobuf-rpc",
    )

    const val POM_PATH = "publications/gpr/pom-default.xml"
    const val RELATIVE_RELEASE_ASSETS_DIRECTORY = "release-assets"
    const val IDE_FALLBACK_SHADOW_JAR_BASE_NAME = "microscript-definition"
    const val IDE_FALLBACK_SHADOW_JAR_CLASSIFIER = "all"
    const val SCRIPT_TEMPLATE_META_INF_PREFIX = "META-INF/kotlin/script/templates"

    fun classEntryName(packagePath: String, simpleName: String): String = "$packagePath/$simpleName.class"

    fun classEntryName(fqcn: String): String {
        val lastDotIndex = fqcn.lastIndexOf('.')
        if (lastDotIndex < 0) {
            return "$fqcn.class"
        }

        val packagePath = fqcn.substring(0, lastDotIndex).replace('.', '/')
        val simpleName = fqcn.substring(lastDotIndex + 1)
        return classEntryName(packagePath, simpleName)
    }

    fun scriptTemplateRegistrationEntry(templateFqcn: String): String =
        "$SCRIPT_TEMPLATE_META_INF_PREFIX/$templateFqcn"

    fun projectCoordinate(dependencyProject: Project): String =
        "${dependencyProject.group}:${dependencyProject.name}"

    fun libraryCoordinate(dependency: ExternalModuleDependency): String =
        "${dependency.group}:${dependency.name}"
}
