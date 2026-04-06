package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspPorts

internal fun allocateDotnetAspPorts(
    artifactId: DotnetAspServiceArtifactId,
    configuredPorts: ResolvedDotnetAspPorts?,
): DotnetAspAllocatedPorts {
    val defaultHttp = dotnetAspHttpPortFor(artifactId)
    val http =
        configuredPorts?.http
            ?: configuredPorts?.https?.minus(HTTPS_PORT_OFFSET)
            ?: defaultHttp
    val https =
        configuredPorts?.https
            ?: configuredPorts?.http?.plus(HTTPS_PORT_OFFSET)
            ?: (defaultHttp + HTTPS_PORT_OFFSET)

    require(http in MIN_DOTNET_ASP_PORT..MAX_DOTNET_ASP_PORT) {
        "ASP.NET HTTP port for '${artifactId.stablePortIdentity()}' must be between " +
            "$MIN_DOTNET_ASP_PORT and $MAX_DOTNET_ASP_PORT."
    }
    require(https in MIN_DOTNET_ASP_PORT..MAX_DOTNET_ASP_PORT) {
        "ASP.NET HTTPS port for '${artifactId.stablePortIdentity()}' must be between " +
            "$MIN_DOTNET_ASP_PORT and $MAX_DOTNET_ASP_PORT."
    }
    require(http != https) {
        "ASP.NET service '${artifactId.stablePortIdentity()}' resolves to the same HTTP and HTTPS port '$http'."
    }

    return DotnetAspAllocatedPorts(http = http, https = https)
}

internal fun dotnetAspHttpPortFor(artifactId: DotnetAspServiceArtifactId): Int {
    val slot =
        artifactId
            .stablePortIdentity()
            .fold(0L) { hash, character ->
                ((hash * PORT_HASH_MULTIPLIER) + character.code) % PORT_SLOT_COUNT
            }.toInt()
    return BASE_HTTP_PORT + (slot * PORT_STRIDE)
}

internal fun validateUniqueDotnetAspPorts(
    servicePorts: List<Pair<DotnetAspServiceArtifactId, DotnetAspAllocatedPorts>>,
) {
    val portOwners = mutableMapOf<Int, MutableList<DotnetAspServiceArtifactId>>()
    servicePorts.forEach { (artifactId, ports) ->
        portOwners.getOrPut(ports.http, ::mutableListOf) += artifactId
        portOwners.getOrPut(ports.https, ::mutableListOf) += artifactId
    }

    val collisions =
        portOwners
            .filterValues { it.size > 1 }
            .toSortedMap()

    require(collisions.isEmpty()) {
        "ASP.NET services produce colliding launch ports: " +
            collisions.entries.joinToString("; ") { (port, artifactIds) ->
                val owners =
                    artifactIds
                        .distinct()
                        .sortedBy(DotnetAspServiceArtifactId::stablePortIdentity)
                        .joinToString(", ") { it.stablePortIdentity() }
                "$owners share localhost:$port"
            } + "."
    }
}

internal fun DotnetAspServiceArtifactId.stablePortIdentity(): String = "$solutionName/$projectName"

private const val BASE_HTTP_PORT = 5_000
private const val PORT_STRIDE = 10
private const val HTTPS_PORT_OFFSET = 1
private const val PORT_SLOT_COUNT = 1_500L
private const val PORT_HASH_MULTIPLIER = 31L
private const val MIN_DOTNET_ASP_PORT = 1
private const val MAX_DOTNET_ASP_PORT = 65_535
