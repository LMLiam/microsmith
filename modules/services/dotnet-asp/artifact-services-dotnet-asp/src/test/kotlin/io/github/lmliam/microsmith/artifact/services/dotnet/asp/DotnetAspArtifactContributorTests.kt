package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.service.DotnetAspServiceContribution
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspHttpMethod
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpointBindings
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModelLocality
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspPorts
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse
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

        "contribute keeps allocated contract type names clear of generated result type names" {
            val endpoint =
                ResolvedDotnetAspEndpoint(
                    method = DotnetAspHttpMethod.GET,
                    route = "/users",
                    routePlaceholders = emptyList(),
                    operationName = "GetUser",
                    bindings =
                        ResolvedDotnetAspEndpointBindings(
                            query = ResolvedDotnetAspRequestBinding("GetUserResult", fields = emptyList())
                        ),
                    responses =
                        listOf(
                            ResolvedDotnetAspResponse(
                                statusCode = 200,
                                model =
                                    ResolvedDotnetAspModel(
                                        ResolvedDotnetAspModelLocality.INLINE,
                                        DotnetModel("GetUserOk", fields = emptyList()),
                                    ),
                                headers = emptyList(),
                            )
                        ),
                )
            val workspace =
                DotnetAspWorkspace(
                    servicesByName =
                        linkedMapOf(
                            "UserService" to
                                resolvedAspService(
                                    "UserService",
                                    "UserService.Api",
                                    rest = ResolvedDotnetAspRest(listOf(endpoint)),
                                )
                        )
                )

            val contribution =
                DotnetAspArtifactContributor().contribute(workspace).single() as DotnetAspServiceContribution

            val allocatedTypeNames =
                contribution.contractModels.map { it.typeName } +
                    listOfNotNull(contribution.endpoints.single().bindings.query?.typeName)
            allocatedTypeNames.intersect(setOf("GetUserResult", "GetUserOk")) shouldBe emptySet()
        }
    })

private fun resolvedAspService(
    name: String,
    projectName: String,
    ports: ResolvedDotnetAspPorts = ResolvedDotnetAspPorts(http = 5000, https = 5001),
    rest: ResolvedDotnetAspRest = ResolvedDotnetAspRest.empty(),
): ResolvedDotnetAspService =
    ResolvedDotnetAspService(
        name = name,
        solutionName = "Platform",
        projectName = projectName,
        targetFrameworkMoniker = "net8.0",
        outputRoot = Path.of("dotnet", "Platform", projectName),
        ports = ports,
        models = emptyMap(),
        rest = rest,
    )
