package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildNames
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
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
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponseHeader
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path

class DotnetAspServiceArtifactCompilerTests :
    StringSpec({
        "compile emits the base ASP.NET scaffold artefacts" {
            val artifact = emptyArtifact()

            val contributions = DotnetAspServiceArtifactCompiler().compile(artifact)
            val msbuild = contributions.filterIsInstance<MsBuildProjectContribution>().single()
            val textFiles = contributions.filterIsInstance<TextFileArtifactContribution>()

            msbuild.artifactId.kind shouldBe MsBuildProjectKind.Project
            msbuild.projectAttributes shouldContainExactly
                mapOf(MsBuildNames.SDK_ATTRIBUTE to "Microsoft.NET.Sdk.Web")
            msbuild.properties shouldContainExactly mapOf(
                MsBuildNames.IMPLICIT_USINGS_PROPERTY to "enable",
                MsBuildNames.NULLABLE_PROPERTY to "enable",
                MsBuildNames.TARGET_FRAMEWORK_PROPERTY to "net8.0",
            )

            textFiles.map { it.artifactId.relativePath.toString() }
                .shouldContainExactlyInAnyOrder(
                    listOf(
                        "Program.cs",
                        "appsettings.json",
                        "Properties/launchSettings.json",
                    ),
                )
            textFiles.forEach {
                it.artifactId.outputRoot shouldBe Path.of("dotnet", "Platform", "UserService.Api")
            }
            textFiles.single { it.artifactId.relativePath.toString() == "Program.cs" }.contents
                .shouldContain("AddControllers")
            textFiles.single { it.artifactId.relativePath.toString() == "Program.cs" }.contents
                .shouldContain("public partial class Program { }")
            textFiles
                .single { it.artifactId.relativePath.toString() == "appsettings.json" }
                .contents
                .shouldContain(
                    "\"ServiceName\": \"UserService\"",
                )
            textFiles
                .single {
                    it.artifactId.relativePath.toString() == "Properties/launchSettings.json"
                }
                .contents
                .shouldContain("http://localhost:5000;https://localhost:5001")
        }

        "compile escapes service names before embedding them in appsettings json" {
            val artifact =
                emptyArtifact(
                    serviceName = "User\"Service\\Api",
                )

            val appSettings =
                DotnetAspServiceArtifactCompiler()
                    .compile(artifact)
                    .filterIsInstance<TextFileArtifactContribution>()
                    .single {
                        it.artifactId.relativePath.toString() == "appsettings.json"
                    }
                    .contents

            appSettings.shouldContain("\"ServiceName\": \"User\\\"Service\\\\Api\"")
            appSettings.shouldNotContain("\"ServiceName\": \"User\"Service\\Api\"")
        }

        "compile emits generated endpoint contracts and abstract controller glue" {
            val userModel =
                DotnetModel(
                    name = "User",
                    fields = listOf(
                        DotnetField("id", DotnetFieldType.String),
                        DotnetField("email", DotnetFieldType.String),
                    ),
                )
            val problemModel =
                DotnetModel(
                    name = "Problem",
                    fields = listOf(DotnetField("message", DotnetFieldType.String)),
                )
            val artifact =
                emptyArtifact(
                    models = mapOf(
                        "Problem" to problemModel,
                        "User" to userModel,
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
                                    headers = ResolvedDotnetAspHeadersBinding(
                                        name = "GetUserHeaders",
                                        headers = listOf(
                                            ResolvedDotnetAspHeaderField(
                                                name = "ifNoneMatch",
                                                headerName = "If-None-Match",
                                            ),
                                        ),
                                    ),
                                ),
                                responses = listOf(
                                    ResolvedDotnetAspResponse(
                                        statusCode = 200,
                                        model = ResolvedDotnetAspModel(
                                            ResolvedDotnetAspModelLocality.SHARED,
                                            userModel,
                                        ),
                                        headers = emptyList(),
                                    ),
                                    ResolvedDotnetAspResponse(
                                        statusCode = 404,
                                        model = ResolvedDotnetAspModel(
                                            ResolvedDotnetAspModelLocality.SHARED,
                                            problemModel,
                                        ),
                                        headers = listOf(
                                            ResolvedDotnetAspResponseHeader("X-Trace-Id"),
                                        ),
                                    ),
                                ),
                            ),
                            ResolvedDotnetAspEndpoint(
                                method = DotnetAspHttpMethod.POST,
                                route = "/users",
                                routePlaceholders = emptyList(),
                                operationName = "CreateUser",
                                bindings = ResolvedDotnetAspEndpointBindings(
                                    query = ResolvedDotnetAspRequestBinding(
                                        name = "CreateUserQuery",
                                        fields = listOf(
                                            ResolvedDotnetAspRequestField(
                                                name = "dryRun",
                                                type = DotnetFieldType.Bool,
                                                optional = true,
                                                defaultValue = false,
                                            ),
                                        ),
                                    ),
                                    body = ResolvedDotnetAspModel(
                                        locality = ResolvedDotnetAspModelLocality.INLINE,
                                        model = DotnetModel(
                                            name = "Body",
                                            fields = listOf(
                                                DotnetField("email", DotnetFieldType.String),
                                            ),
                                        ),
                                    ),
                                ),
                                responses = listOf(
                                    ResolvedDotnetAspResponse(
                                        statusCode = 201,
                                        model = ResolvedDotnetAspModel(
                                            ResolvedDotnetAspModelLocality.SHARED,
                                            userModel,
                                        ),
                                        headers = listOf(
                                            ResolvedDotnetAspResponseHeader("Location"),
                                        ),
                                    ),
                                    ResolvedDotnetAspResponse(
                                        statusCode = 400,
                                        model = ResolvedDotnetAspModel(
                                            locality = ResolvedDotnetAspModelLocality.INLINE,
                                            model = DotnetModel(
                                                name = "Problem",
                                                fields = listOf(
                                                    DotnetField("message", DotnetFieldType.String),
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

            textFiles.keys shouldContainExactlyInAnyOrder listOf(
                "Program.cs",
                "appsettings.json",
                "Properties/launchSettings.json",
                "Generated/Contracts/ServiceModels.cs",
                "Generated/Contracts/RequestModels.cs",
                "Generated/Contracts/ResponseModels.cs",
                "Generated/Controllers/UserServiceApiControllerBase.cs",
            )

            textFiles
                .getValue("Generated/Contracts/ServiceModels.cs")
                .contents
                .shouldContain("public sealed class User")
            textFiles.getValue("Generated/Contracts/RequestModels.cs").contents
                .shouldContain("public sealed class GetUserPath")
            textFiles.getValue("Generated/Contracts/RequestModels.cs").contents
                .shouldContain("public bool DryRun { get; set; } = false;")
            textFiles.getValue("Generated/Contracts/RequestModels.cs").contents
                .shouldContain("public sealed class CreateUserBody")
            textFiles.getValue("Generated/Contracts/ResponseModels.cs").contents
                .shouldContain("public abstract record GetUserResult;")
            textFiles.getValue("Generated/Contracts/ResponseModels.cs").contents
                .shouldContain(
                    "public sealed record CreateUserCreated(" +
                        "User Body, string? Location = null" +
                        ") : CreateUserResult;",
                )
            textFiles.getValue("Generated/Contracts/ResponseModels.cs").contents
                .shouldContain("public sealed class CreateUserBadRequestProblem")
            textFiles.getValue("Generated/Controllers/UserServiceApiControllerBase.cs").contents
                .shouldContain("[HttpGet(\"/users/{id}\", Name = \"GetUser\")]")
            textFiles.getValue("Generated/Controllers/UserServiceApiControllerBase.cs").contents
                .shouldContain("var headers = new GetUserHeaders")
            textFiles.getValue("Generated/Controllers/UserServiceApiControllerBase.cs").contents
                .shouldContain("protected abstract Task<GetUserResult> OnGetUserAsync(")
            textFiles.getValue("Generated/Controllers/UserServiceApiControllerBase.cs").contents
                .shouldContain("CreateUserBadRequest response => Respond(response.Body, 400)")
        }

        "compile emits assignable literals for ushort request defaults" {
            val artifact =
                emptyArtifact(
                    rest = ResolvedDotnetAspRest(
                        listOf(
                            ResolvedDotnetAspEndpoint(
                                method = DotnetAspHttpMethod.GET,
                                route = "/users",
                                routePlaceholders = emptyList(),
                                operationName = "ListUsers",
                                bindings = ResolvedDotnetAspEndpointBindings(
                                    query = ResolvedDotnetAspRequestBinding(
                                        name = "ListUsersQuery",
                                        fields = listOf(
                                            ResolvedDotnetAspRequestField(
                                                name = "rank",
                                                type = DotnetFieldType.UnsignedShort,
                                                optional = true,
                                                defaultValue = 1,
                                            ),
                                        ),
                                    ),
                                ),
                                responses = listOf(
                                    sharedResponse(
                                        statusCode = 200,
                                        modelName = "User",
                                    ),
                                ),
                            ),
                        ),
                    ),
                    models = singleStringModel("User"),
                )

            val requestModels =
                DotnetAspServiceArtifactCompiler()
                    .compile(artifact)
                    .filterIsInstance<TextFileArtifactContribution>()
                    .single {
                        it.artifactId.relativePath.toString() == "Generated/Contracts/RequestModels.cs"
                    }
                    .contents

            requestModels.shouldContain("public ushort Rank { get; set; } = 1;")
            requestModels.shouldNotContain("public ushort Rank { get; set; } = 1u;")
        }

        "compile sanitizes response header names into valid csharp identifiers" {
            val artifact =
                emptyArtifact(
                    models = singleStringModel("User"),
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
                                    sharedResponse(
                                        statusCode = 200,
                                        modelName = "User",
                                        headers = listOf("X.Trace-Id"),
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

            textFiles.getValue("Generated/Contracts/ResponseModels.cs").contents
                .shouldContain("string? XTraceId = null")
            textFiles.getValue("Generated/Controllers/UserServiceApiControllerBase.cs").contents
                .shouldContain("response.XTraceId")
        }

        "compile rejects response headers that collide after csharp identifier sanitization" {
            val artifact =
                emptyArtifact(
                    models = singleStringModel("User"),
                    rest = ResolvedDotnetAspRest(
                        listOf(
                            ResolvedDotnetAspEndpoint(
                                method = DotnetAspHttpMethod.GET,
                                route = "/users",
                                routePlaceholders = emptyList(),
                                operationName = "GetUser",
                                bindings = ResolvedDotnetAspEndpointBindings(),
                                responses = listOf(
                                    sharedResponse(
                                        statusCode = 200,
                                        modelName = "User",
                                        headers = listOf("X-Trace-Id", "X.Trace Id"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                )

            shouldThrow<IllegalArgumentException> {
                DotnetAspServiceArtifactCompiler().compile(artifact)
            }.message.shouldContain("colliding generated property names")
        }
    })

private fun emptyArtifact(
    serviceName: String = "UserService",
    models: Map<String, DotnetModel> = emptyMap(),
    rest: ResolvedDotnetAspRest = ResolvedDotnetAspRest.empty(),
): DotnetAspServiceArtifact = DotnetAspServiceArtifact(
    id = DotnetAspServiceArtifactId(solutionName = "Platform", projectName = "UserService.Api"),
    serviceName = serviceName,
    targetFrameworkMoniker = "net8.0",
    outputRoot = Path.of("dotnet", "Platform", "UserService.Api"),
    httpPort = 5000,
    httpsPort = 5001,
    models = models,
    rest = rest,
)

private fun singleStringModel(modelName: String): Map<String, DotnetModel> = mapOf(
    modelName to DotnetModel(
        name = modelName,
        fields = listOf(DotnetField("id", DotnetFieldType.String)),
    ),
)

private fun sharedResponse(
    statusCode: Int,
    modelName: String,
    headers: List<String> = emptyList(),
): ResolvedDotnetAspResponse = ResolvedDotnetAspResponse(
    statusCode = statusCode,
    model = ResolvedDotnetAspModel(
        locality = ResolvedDotnetAspModelLocality.SHARED,
        model = requireNotNull(singleStringModel(modelName)[modelName]),
    ),
    headers = headers.map(::ResolvedDotnetAspResponseHeader),
)
