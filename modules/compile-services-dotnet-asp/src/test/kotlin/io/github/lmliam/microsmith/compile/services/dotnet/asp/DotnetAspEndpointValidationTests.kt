package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspEndpointBindingsArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelLocality
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspRequestBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspRequestFieldArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspResponseArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspResponseHeaderArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifactId
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import java.nio.file.Path

class DotnetAspEndpointValidationTests :
    StringSpec({
        "validation rejects path and query bindings that reference models" {
            val userModel = sharedModel("User")
            val artifact =
                validationArtifact(
                    contractModels = listOf(userModel),
                    endpoints = listOf(
                        DotnetAspEndpointArtifact(
                            method = "GET",
                            route = "/users/{id}",
                            operationName = "GetUser",
                            bindings = DotnetAspEndpointBindingsArtifact(
                                query = DotnetAspRequestBindingArtifact(
                                    typeName = "GetUserQuery",
                                    name = "GetUserQuery",
                                    fields = listOf(
                                        DotnetAspRequestFieldArtifact(
                                            name = "user",
                                            type = DotnetFieldType.Reference("User"),
                                            optional = false,
                                            defaultValue = null,
                                        ),
                                    ),
                                    origins = setOf("services.UserService.rest.GetUser.query.GetUserQuery"),
                                ),
                            ),
                            responses = listOf(
                                DotnetAspResponseArtifact(
                                    statusCode = 200,
                                    model = userModel,
                                    headers = emptyList(),
                                    origins = setOf("services.UserService.rest.GetUser.responses.200"),
                                ),
                            ),
                            origins = setOf("services.UserService.rest.GetUser"),
                        ),
                    ),
                )

            val error =
                shouldThrow<IllegalArgumentException> {
                    validateEndpointGenerationInputs(artifact)
                }

            error.message.shouldContain("Transport bindings must declare scalar fields")
            error.message.shouldContain("cannot reference shared model 'User'")
        }

        "validation rejects response headers that collide with the generated Body property" {
            val userModel = sharedModel("User")
            val artifact =
                validationArtifact(
                    contractModels = listOf(userModel),
                    endpoints = listOf(
                        DotnetAspEndpointArtifact(
                            method = "GET",
                            route = "/users/{id}",
                            operationName = "GetUser",
                            bindings = DotnetAspEndpointBindingsArtifact(),
                            responses = listOf(
                                DotnetAspResponseArtifact(
                                    statusCode = 200,
                                    model = userModel,
                                    headers = listOf(DotnetAspResponseHeaderArtifact("body")),
                                    origins = setOf("services.UserService.rest.GetUser.responses.200"),
                                ),
                            ),
                            origins = setOf("services.UserService.rest.GetUser"),
                        ),
                    ),
                )

            val error =
                shouldThrow<IllegalArgumentException> {
                    validateEndpointGenerationInputs(artifact)
                }

            error.message.shouldContain("collides with the generated result body property 'Body'")
            error.message.shouldContain("header 'body'")
        }

        "validation rejects project names that collide with the shared controller base type" {
            val artifact = validationArtifact(projectName = "Microsmith")

            val error =
                shouldThrow<IllegalArgumentException> {
                    validateEndpointGenerationInputs(artifact)
                }

            error.message.shouldContain("collides with shared generated controller base type")
            error.message.shouldContain("project 'Microsmith'")
        }

        "validation rejects colliding generated contract types" {
            val sharedResultModel = sharedModel("GetUserResult")
            val artifact =
                validationArtifact(
                    contractModels = listOf(sharedResultModel, sharedModel("User")),
                    endpoints = listOf(
                        DotnetAspEndpointArtifact(
                            method = "GET",
                            route = "/users/{id}",
                            operationName = "GetUser",
                            bindings = DotnetAspEndpointBindingsArtifact(),
                            responses = listOf(
                                DotnetAspResponseArtifact(
                                    statusCode = 200,
                                    model = sharedModel("User"),
                                    headers = emptyList(),
                                    origins = setOf("services.UserService.rest.GetUser.responses.200"),
                                ),
                            ),
                            origins = setOf("services.UserService.rest.GetUser"),
                        ),
                    ),
                )

            val error =
                shouldThrow<IllegalArgumentException> {
                    validateEndpointGenerationInputs(artifact)
                }

            error.message.shouldContain("colliding generated contract types")
            error.message.shouldContain("GetUserResult")
            error.message.shouldContain("generated contract model 'GetUserResult'")
        }
    })

private fun validationArtifact(
    contractModels: List<DotnetAspModelArtifact> = emptyList(),
    endpoints: List<DotnetAspEndpointArtifact> = emptyList(),
    projectName: String = "UserService.Api",
): DotnetAspServiceArtifact = DotnetAspServiceArtifact(
    id = DotnetAspServiceArtifactId("Platform", projectName),
    serviceName = "UserService",
    targetFrameworkMoniker = "net8.0",
    outputRoot = Path.of("dotnet", "Platform", "UserService.Api"),
    httpPort = 5000,
    httpsPort = 5001,
    contractModels = contractModels,
    endpoints = endpoints,
)

private fun sharedModel(name: String): DotnetAspModelArtifact = DotnetAspModelArtifact(
    typeName = name,
    locality = DotnetAspModelLocality.SHARED,
    model = DotnetModel(name = name, fields = listOf(DotnetField("id", DotnetFieldType.String))),
    origins = setOf("services.UserService.models.$name"),
)
