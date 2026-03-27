package io.github.lmliam.microsmith.dsl.services.dotnet.packages.support

import io.github.lmliam.microsmith.dsl.services.dotnet.packages.service.DotnetPackageReferenceDeclaration
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution.DotnetPackageVersionDeclaration

internal fun normalizeDotnetPackagePath(value: String, label: String): List<String> {
    val normalized = value.trim()
    require(normalized.isNotBlank()) { "$label cannot be blank." }
    require(!normalized.startsWith('.')) { "$label cannot start with '.'." }
    require(!normalized.endsWith('.')) { "$label cannot end with '.'." }

    val segments = normalized.split('.')
    require(segments.all { it.isNotBlank() }) { "$label cannot contain empty path segments: '$value'." }
    require(segments.all(::isValidDotnetPackageSegment)) {
        "$label is not a valid .NET package identifier: '$value'"
    }

    return segments
}

internal fun validateDotnetPackageVersion(value: String, label: String): String {
    val normalized = value.trim()
    require(normalized.isNotBlank()) { "$label cannot be blank." }
    require(normalized.none(Char::isWhitespace)) { "$label cannot contain whitespace: '$value'" }
    require(
        normalized.all { character ->
            character.isLetterOrDigit() || character == '.' || character == '-' || character == '+' || character == '_'
        },
    ) {
        "$label is not a valid .NET package version: '$value'"
    }

    return normalized
}

internal fun flattenOwnedPackages(root: DotnetPackageDeclarationNode): List<DotnetPackageVersionDeclaration> {
    val packages = linkedMapOf<String, DotnetPackageVersionDeclaration>()

    fun visit(node: DotnetPackageDeclarationNode, inheritedVersion: String?) {
        val currentVersion = node.version ?: inheritedVersion

        if (node.pathSegments.isNotEmpty() && node.childPackages.isEmpty()) {
            val packageName = node.pathSegments.joinToString(".")
            val version =
                currentVersion
                    ?: error("Dotnet package '$packageName' must declare a version.")

            require(packageName !in packages) {
                "Duplicate .NET package ownership declaration for '$packageName'."
            }

            packages[packageName] = DotnetPackageVersionDeclaration(name = packageName, version = version)
            return
        }

        node.childPackages.forEach { child ->
            visit(child, currentVersion)
        }
    }

    visit(root, null)
    return packages.values.toList()
}

internal fun flattenReferencedPackages(root: DotnetPackageDeclarationNode): List<DotnetPackageReferenceDeclaration> {
    val packages = linkedMapOf<String, DotnetPackageReferenceDeclaration>()

    fun visit(node: DotnetPackageDeclarationNode, inheritedVersion: String?) {
        val currentVersion = node.version ?: inheritedVersion

        if (node.pathSegments.isNotEmpty() && node.childPackages.isEmpty()) {
            val packageName = node.pathSegments.joinToString(".")

            require(packageName !in packages) {
                "Duplicate .NET package reference declaration for '$packageName'."
            }

            packages[packageName] = DotnetPackageReferenceDeclaration(name = packageName, version = currentVersion)
            return
        }

        node.childPackages.forEach { child ->
            visit(child, currentVersion)
        }
    }

    visit(root, null)
    return packages.values.toList()
}

private fun isValidDotnetPackageSegment(value: String): Boolean {
    if (value.isEmpty()) {
        return false
    }

    return value.all { character ->
        character.isLetterOrDigit() || character == '-' || character == '_'
    }
}
