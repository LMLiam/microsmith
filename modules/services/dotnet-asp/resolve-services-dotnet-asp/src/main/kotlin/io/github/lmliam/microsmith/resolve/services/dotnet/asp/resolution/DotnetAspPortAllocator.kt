package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspPorts
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue.PortKind

internal data class DotnetAspPortRequest(
    val serviceName: String,
    val solutionName: String,
    val projectName: String,
    val http: Int?,
    val https: Int?,
) {
    val stablePortIdentity: String = "$solutionName/$projectName"
    val isConfigured: Boolean = http != null || https != null
}

internal object DotnetAspPortAllocator {
    fun allocate(
        requests: List<DotnetAspPortRequest>
    ): EitherNel<DotnetAspResolutionIssue, Map<String, ResolvedDotnetAspPorts>> {
        val (configuredRequests, defaultedRequests) = requests.partition(DotnetAspPortRequest::isConfigured)
        val configured = configuredRequests.associate { it.serviceName to it.deriveConfiguredPorts() }

        val rangeIssues = configuredRequests.flatMap { request ->
            val ports = configured.getValue(request.serviceName)
            listOfNotNull(
                ports.http.takeUnless(::isValidPort)?.let {
                    DotnetAspResolutionIssue.PortOutOfRange(request.serviceName, PortKind.HTTP, it)
                },
                ports.https.takeUnless(::isValidPort)?.let {
                    DotnetAspResolutionIssue.PortOutOfRange(request.serviceName, PortKind.HTTPS, it)
                },
            )
        }

        val collisionIssues = launchPortCollisions(configured)

        val reservedPorts = configured.values.flatMap { listOf(it.http, it.https) }.toMutableSet()
        val defaulted = mutableMapOf<String, ResolvedDotnetAspPorts>()
        val exhaustedIssues = mutableListOf<DotnetAspResolutionIssue>()
        defaultedRequests.sortedBy(DotnetAspPortRequest::stablePortIdentity).forEach { request ->
            val ports = firstFreeDefaultPorts(request, reservedPorts)
            if (ports == null) {
                exhaustedIssues += DotnetAspResolutionIssue.NoFreeDefaultPorts(request.serviceName)
            } else {
                reservedPorts += listOf(ports.http, ports.https)
                defaulted[request.serviceName] = ports
            }
        }

        val issues = rangeIssues + collisionIssues + exhaustedIssues
        return issues.toNonEmptyListOrNull()?.let { Either.Left(it) } ?: Either.Right(configured + defaulted)
    }

    private fun DotnetAspPortRequest.deriveConfiguredPorts(): ResolvedDotnetAspPorts =
        ResolvedDotnetAspPorts(
            http = http ?: requireNotNull(https).minus(HTTPS_PORT_OFFSET),
            https = https ?: requireNotNull(http).plus(HTTPS_PORT_OFFSET),
        )

    private fun launchPortCollisions(configured: Map<String, ResolvedDotnetAspPorts>): List<DotnetAspResolutionIssue> =
        configured.entries
            .flatMap { (serviceName, ports) ->
                listOf(ports.http to serviceName, ports.https to serviceName).filter { isValidPort(it.first) }
            }
            .groupBy({ it.first }, { it.second })
            .filterValues { it.size > 1 }
            .toSortedMap()
            .map { (port, owners) -> DotnetAspResolutionIssue.LaunchPortCollision(port, owners.sorted()) }

    private fun firstFreeDefaultPorts(request: DotnetAspPortRequest, reservedPorts: Set<Int>): ResolvedDotnetAspPorts? {
        val firstSlot = defaultSlot(request.stablePortIdentity)
        return (0 until PORT_SLOT_COUNT)
            .asSequence()
            .map { offset -> httpPortForSlot((firstSlot + offset) % PORT_SLOT_COUNT) }
            .map { http -> ResolvedDotnetAspPorts(http = http, https = http + HTTPS_PORT_OFFSET) }
            .firstOrNull { it.http !in reservedPorts && it.https !in reservedPorts }
    }

    private fun defaultSlot(stablePortIdentity: String): Int =
        stablePortIdentity
            .fold(0L) { hash, character -> ((hash * PORT_HASH_MULTIPLIER) + character.code) % PORT_SLOT_COUNT }
            .toInt()

    private fun httpPortForSlot(slot: Int): Int = BASE_HTTP_PORT + (slot * PORT_STRIDE)

    private fun isValidPort(port: Int): Boolean = port in MIN_PORT..MAX_PORT

    private const val BASE_HTTP_PORT = 5_000
    private const val PORT_STRIDE = 10
    private const val HTTPS_PORT_OFFSET = 1
    private const val PORT_SLOT_COUNT = 1_500
    private const val PORT_HASH_MULTIPLIER = 31L
    private const val MIN_PORT = 1
    private const val MAX_PORT = 65_535
}
