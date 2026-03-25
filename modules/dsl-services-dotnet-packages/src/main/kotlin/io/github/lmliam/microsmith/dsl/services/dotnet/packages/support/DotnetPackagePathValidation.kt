package io.github.lmliam.microsmith.dsl.services.dotnet.packages.support

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

internal fun flattenOwnedPackages(root: DotnetPackageNode): Map<String, String> {
    val packages = linkedMapOf<String, String>()

    fun visit(node: DotnetPackageNode, inheritedVersion: String?) {
        val currentVersion = node.version ?: inheritedVersion

        if (node.pathSegments.isNotEmpty() && node.children.isEmpty()) {
            val packageName = node.pathSegments.joinToString(".")
            val version =
                currentVersion
                    ?: error("Dotnet package '$packageName' must declare a version.")

            require(packageName !in packages) {
                "Duplicate .NET package ownership declaration for '$packageName'."
            }

            packages[packageName] = version
            return
        }

        node.children.forEach { child ->
            visit(child, currentVersion)
        }
    }

    visit(root, null)
    return packages
}

internal fun flattenReferencedPackages(root: DotnetPackageNode): Set<String> {
    val packages = linkedSetOf<String>()

    fun visit(node: DotnetPackageNode) {
        if (node.pathSegments.isNotEmpty() && node.children.isEmpty()) {
            val packageName = node.pathSegments.joinToString(".")

            require(packageName !in packages) {
                "Duplicate .NET package reference declaration for '$packageName'."
            }

            packages += packageName
            return
        }

        node.children.forEach { child ->
            visit(child)
        }
    }

    visit(root)
    return packages
}

private fun isValidDotnetPackageSegment(value: String): Boolean {
    if (value.isEmpty()) {
        return false
    }

    return value.all { character ->
        character.isLetterOrDigit() || character == '-' || character == '_'
    }
}
