package io.github.lmliam.microsmith.artifact.services.dotnet.asp

internal fun dotnetAspHttpPortFor(artifactId: DotnetAspServiceArtifactId): Int {
    val slot =
        artifactId
            .stablePortIdentity()
            .fold(0L) { hash, character ->
                ((hash * PORT_HASH_MULTIPLIER) + character.code) % PORT_SLOT_COUNT
            }.toInt()
    return BASE_HTTP_PORT + (slot * PORT_STRIDE)
}

internal fun validateUniqueDotnetAspPorts(serviceIds: List<DotnetAspServiceArtifactId>) {
    val collisions =
        serviceIds
            .groupBy(::dotnetAspHttpPortFor)
            .filterValues { it.size > 1 }
            .toSortedMap()
    require(collisions.isEmpty()) {
        "ASP.NET services produce colliding generated launch ports: " +
            collisions.entries.joinToString("; ") { (httpPort, artifactIds) ->
                val httpsPort = httpPort + HTTPS_PORT_OFFSET
                val owners =
                    artifactIds
                        .sortedBy(DotnetAspServiceArtifactId::stablePortIdentity)
                        .joinToString(", ") { it.stablePortIdentity() }
                "$owners " +
                    "share http://localhost:$httpPort and https://localhost:$httpsPort"
            } + "."
    }
}

private fun DotnetAspServiceArtifactId.stablePortIdentity(): String = "$solutionName/$projectName"

private const val BASE_HTTP_PORT = 5_000
private const val PORT_STRIDE = 10
private const val HTTPS_PORT_OFFSET = 1
private const val PORT_SLOT_COUNT = 1_500L
private const val PORT_HASH_MULTIPLIER = 31L
