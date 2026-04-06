package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifactId
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.endpoint.DotnetAspHttpMethod
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpointBindings
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspHeaderField
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspHeadersBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModelLocality
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestField
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class DotnetAspGeneratedGoldenTests :
    StringSpec({
        "compile keeps hosting extensions output stable" {
            val artifact = goldenArtifact(rest = ResolvedDotnetAspRest.empty())

            val hostingFile =
                DotnetAspServiceArtifactCompiler()
                    .compile(artifact)
                    .filterIsInstance<TextFileArtifactContribution>()
                    .single {
                        it.artifactId.relativePath.toString() ==
                            "Generated/Hosting/MicrosmithHostingExtensions.cs"
                    }
                    .contents

            hostingFile shouldBe goldenResource("golden/MicrosmithHostingExtensions.cs")
        }

        "compile keeps header-binding controller output stable" {
            val artifact =
                goldenArtifact(
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
                                    headers = ResolvedDotnetAspHeadersBinding(
                                        name = "GetUserHeaders",
                                        headers = listOf(
                                            ResolvedDotnetAspHeaderField(
                                                name = "xCorrelationId",
                                                headerName = "X-Correlation-Id",
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
                                                fields = listOf(
                                                    DotnetField("id", DotnetFieldType.String),
                                                ),
                                            ),
                                        ),
                                        headers = emptyList(),
                                    ),
                                ),
                            ),
                        ),
                    ),
                )
            val textFiles =
                DotnetAspServiceArtifactCompiler()
                    .compile(artifact)
                    .filterIsInstance<TextFileArtifactContribution>()
                    .associateBy { it.artifactId.relativePath.toString() }

            textFiles.getValue("Generated/Contracts/RequestModels.cs").contents shouldBe
                goldenResource("golden/GetUserRequestModels.cs")
            textFiles.getValue("Generated/Controllers/UserServiceApiControllerBase.cs").contents shouldBe
                goldenResource("golden/GetUserControllerBase.cs")
        }
    })

private fun goldenArtifact(rest: ResolvedDotnetAspRest): DotnetAspServiceArtifact = DotnetAspServiceArtifact(
    id = DotnetAspServiceArtifactId("Platform", "UserService.Api"),
    serviceName = "UserService",
    targetFrameworkMoniker = "net8.0",
    outputRoot = Path.of("dotnet", "Platform", "UserService.Api"),
    httpPort = 5000,
    httpsPort = 5001,
    models =
    mapOf(
        "User" to DotnetModel(
            name = "User",
            fields = listOf(DotnetField("id", DotnetFieldType.String)),
        ),
    ),
    rest = rest,
)

private fun goldenResource(path: String): String =
    requireNotNull(DotnetAspGeneratedGoldenTests::class.java.getResourceAsStream(path)) {
        "Missing golden resource '$path'."
    }.readBytes().toString(StandardCharsets.UTF_8)
