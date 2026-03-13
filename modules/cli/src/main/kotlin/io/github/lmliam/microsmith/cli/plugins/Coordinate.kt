package io.github.lmliam.microsmith.cli.plugins

private const val COORDINATE_PART_COUNT = 3

internal data class Coordinate(val group: String, val artifact: String, val version: String) {
    val value: String
        get() = "$group:$artifact:$version"

    val relativeJarPath: String
        get() = "${group.replace('.', '/')}/$artifact/$version/$artifact-$version.jar"
}

internal fun parseCoordinate(raw: String): Coordinate {
    val parts = raw.split(':')
    require(parts.size == COORDINATE_PART_COUNT && parts.none { it.isBlank() }) {
        "Invalid --plugin value '$raw'. Expected group:artifact:version."
    }
    val group = parts[0]
    require(!group.startsWith("io.github.lmliam.microsmith")) {
        "Built-in Microsmith dependencies are pinned in the CLI distribution. " +
            "Use external plugin coordinates only."
    }

    val artifact = parts[1]
    val version = parts[2]
    validateCoordinateGroup(group)
    validateCoordinateSegment("artifact", artifact)
    validateCoordinateSegment("version", version)

    return Coordinate(group = group, artifact = artifact, version = version)
}

private fun validateCoordinateGroup(group: String) {
    val segments = group.split('.')
    require(segments.none(String::isBlank)) {
        "Plugin coordinate group '$group' contains an empty package segment."
    }
    segments.forEach { segment ->
        validateCoordinateSegment("group", segment)
    }
}

private fun validateCoordinateSegment(label: String, value: String) {
    require(!value.contains('/') && !value.contains('\\')) {
        "Plugin coordinate $label '$value' contains a path separator."
    }
    require(value != "." && value != "..") {
        "Plugin coordinate $label '$value' contains an invalid path segment."
    }
    require(!value.contains('|')) {
        "Plugin coordinate $label '$value' contains a reserved lockfile delimiter."
    }
}
