package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.service.DotnetAspServiceContribution
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspPorts
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRest
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspService
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution.DotnetAspWorkspace
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path

class DotnetAspArtifactContributorTests :
    StringSpec({
        "contribute copies the resolved launch ports" {
            val contributor = DotnetAspArtifactContributor()
            val workspace =
                DotnetAspWorkspace(
                    servicesByName =
                        linkedMapOf(
                            "UserService" to
                                resolvedAspService(
                                    name = "UserService",
                                    projectName = "UserService.Api",
                                    ports = ResolvedDotnetAspPorts(http = 7000, https = 7443),
                                )
                        )
                )

            val contribution = contributor.contribute(workspace).single() as DotnetAspServiceContribution

            contribution.httpPort shouldBe 7000
            contribution.httpsPort shouldBe 7443
        }
    })

private fun resolvedAspService(
    name: String,
    projectName: String,
    ports: ResolvedDotnetAspPorts = ResolvedDotnetAspPorts(http = 5000, https = 5001),
): ResolvedDotnetAspService =
    ResolvedDotnetAspService(
        name = name,
        solutionName = "Platform",
        projectName = projectName,
        targetFrameworkMoniker = "net8.0",
        outputRoot = Path.of("dotnet", "Platform", projectName),
        ports = ports,
        models = emptyMap(),
        rest = ResolvedDotnetAspRest.empty(),
    )
