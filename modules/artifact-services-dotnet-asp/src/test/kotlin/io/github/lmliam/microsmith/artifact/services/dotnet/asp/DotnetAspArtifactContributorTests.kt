package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import io.github.lmliam.microsmith.resolve.services.dotnet.asp.DotnetAspWorkspace
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRest
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Path

class DotnetAspArtifactContributorTests :
    StringSpec({
        "contribute keeps existing service ports stable when unrelated services are added" {
            val contributor = DotnetAspArtifactContributor()
            val userOnlyWorkspace =
                DotnetAspWorkspace(
                    servicesByName =
                    linkedMapOf(
                        "UserService" to resolvedAspService("UserService", "UserService.Api"),
                    ),
                )
            val expandedWorkspace =
                DotnetAspWorkspace(
                    servicesByName =
                    linkedMapOf(
                        "AdminService" to resolvedAspService("AdminService", "AdminService.Api"),
                        "UserService" to resolvedAspService("UserService", "UserService.Api"),
                    ),
                )

            val userOnlyPort =
                contributor
                    .contribute(userOnlyWorkspace)
                    .single()
                    .let { it as DotnetAspServiceContribution }
                    .httpPort
            val expandedUserPort =
                contributor
                    .contribute(expandedWorkspace)
                    .map { it as DotnetAspServiceContribution }
                    .single { it.artifactId.projectName == "UserService.Api" }
                    .httpPort

            userOnlyPort shouldBe expandedUserPort
        }

        "contribute rejects stable launch-port collisions instead of silently reshuffling services" {
            val collision = requireNotNull(findCollidingServiceIds())
            val contributor = DotnetAspArtifactContributor()
            val workspace =
                DotnetAspWorkspace(
                    servicesByName =
                    linkedMapOf(
                        "LeftService" to resolvedAspService("LeftService", collision.first.projectName),
                        "RightService" to resolvedAspService("RightService", collision.second.projectName),
                    ),
                )

            val error =
                shouldThrow<IllegalArgumentException> {
                    contributor.contribute(workspace)
                }

            error.message.shouldContain("colliding generated launch ports")
        }
    })

private fun resolvedAspService(name: String, projectName: String): ResolvedDotnetAspService {
    return ResolvedDotnetAspService(
        name = name,
        solutionName = "Platform",
        projectName = projectName,
        targetFrameworkMoniker = "net8.0",
        outputRoot = Path.of("dotnet", "Platform", projectName),
        models = emptyMap(),
        rest = ResolvedDotnetAspRest.empty(),
    )
}

private fun findCollidingServiceIds(): Pair<DotnetAspServiceArtifactId, DotnetAspServiceArtifactId>? {
    val byPort = mutableMapOf<Int, DotnetAspServiceArtifactId>()
    for (index in 0..10_000) {
        val artifactId = DotnetAspServiceArtifactId("Platform", "Collision$index.Api")
        val existing = byPort.putIfAbsent(dotnetAspHttpPortFor(artifactId), artifactId)
        if (existing != null) {
            return existing to artifactId
        }
    }
    return null
}
