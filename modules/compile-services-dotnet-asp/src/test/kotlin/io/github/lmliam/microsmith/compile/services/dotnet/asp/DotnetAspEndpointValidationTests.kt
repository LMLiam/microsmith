package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifactId
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.endpoint.DotnetAspHttpMethod
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpointBindings
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModelLocality
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestField
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import java.nio.file.Path

class DotnetAspEndpointValidationTests :
    StringSpec({
        "validation rejects generated contract type collisions" {
            val artifact =
                validationArtifact(
                    models =
                    mapOf(
                        "GetUserResult" to DotnetModel(
                            name = "GetUserResult",
                            fields = listOf(DotnetField("id", DotnetFieldType.String)),
                        ),
                    ),
                    rest = ResolvedDotnetAspRest(
                        listOf(
                            ResolvedDotnetAspEndpoint(
                                method = DotnetAspHttpMethod.GET,
                                route = "/users/{id}",
                                routePlaceholders = listOf("id"),
                                operationName = "GetUser",
                                bindings = ResolvedDotnetAspEndpointBindings(
                                    path = ResolvedDotnetAspRequestBinding(
                                        name = "GetUserPath",
                                        fields = listOf(
                                            ResolvedDotnetAspRequestField(
                                                name = "id",
                                                type = DotnetFieldType.String,
                                                optional = false,
                                                defaultValue = null,
                                            ),
                                        ),
                                    ),
                                ),
                                responses = listOf(
                                    ResolvedDotnetAspResponse(
                                        statusCode = 200,
                                        model = ResolvedDotnetAspModel(
                                            locality = ResolvedDotnetAspModelLocality.SHARED,
                                            model = DotnetModel(
                                                name = "User",
                                                fields = listOf(DotnetField("id", DotnetFieldType.String)),
                                            ),
                                        ),
                                        headers = emptyList(),
                                    ),
                                ),
                            ),
                        ),
                    ),
                )

            val error =
                shouldThrow<IllegalArgumentException> {
                    validateEndpointGenerationInputs(artifact)
                }

            error.message.shouldContain("colliding generated contract types")
            error.message.shouldContain("GetUserResult")
            error.message.shouldContain("shared model 'GetUserResult'")
            error.message.shouldContain("result base for operation 'GetUser'")
        }
    })

private fun validationArtifact(
    models: Map<String, DotnetModel>,
    rest: ResolvedDotnetAspRest,
): DotnetAspServiceArtifact = DotnetAspServiceArtifact(
    id = DotnetAspServiceArtifactId("Platform", "UserService.Api"),
    serviceName = "UserService",
    targetFrameworkMoniker = "net8.0",
    outputRoot = Path.of("dotnet", "Platform", "UserService.Api"),
    httpPort = 5000,
    httpsPort = 5001,
    models = models,
    rest = rest,
)
