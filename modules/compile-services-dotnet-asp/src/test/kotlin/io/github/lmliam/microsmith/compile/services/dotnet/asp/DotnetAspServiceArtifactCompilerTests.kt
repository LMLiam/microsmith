package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspEndpointBindingsArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspHeaderFieldArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspHeadersBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelLocality
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspRequestBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspRequestFieldArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspResponseArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspResponseHeaderArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildNames
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path

class DotnetAspServiceArtifactCompilerTests :
    StringSpec({
        "compile emits the abstract ASP.NET extension surface" {
            val artifact = sampleArtifact()

            val contributions = DotnetAspServiceArtifactCompiler().compile(artifact)
            val msbuild = contributions.filterIsInstance<MsBuildProjectContribution>().single()
            val textFiles = contributions.filterIsInstance<TextFileArtifactContribution>()
            val byPath = textFiles.associateBy { it.artifactId.relativePath.toString() }

            msbuild.artifactId.kind shouldBe MsBuildProjectKind.Project
            msbuild.projectAttributes shouldBe mapOf(
                MsBuildNames.SDK_ATTRIBUTE to "Microsoft.NET.Sdk.Web",
            )
            msbuild.properties shouldBe mapOf(
                MsBuildNames.IMPLICIT_USINGS_PROPERTY to "enable",
                MsBuildNames.NULLABLE_PROPERTY to "enable",
                MsBuildNames.TARGET_FRAMEWORK_PROPERTY to "net8.0",
            )
            msbuild.origins shouldBe setOf("services.UserService")

            textFiles.map { it.artifactId.relativePath.toString() } shouldContainExactlyInAnyOrder listOf(
                "Program.cs",
                "appsettings.json",
                "Properties/launchSettings.json",
                "Generated/Hosting/MicrosmithHostingExtensions.cs",
                "Generated/Contracts/ServiceModels.cs",
                "Generated/Contracts/RequestModels.cs",
                "Generated/Contracts/ResponseModels.cs",
                "Generated/Controllers/MicrosmithControllerBase.cs",
                "Generated/Controllers/UserServiceApiControllerBase.cs",
            )
            textFiles.forEach {
                it.artifactId.outputRoot shouldBe Path.of("dotnet", "Platform", "UserService.Api")
            }

            byPath.getValue("Program.cs").contents.shouldContain("builder.AddMicrosmith();")
            byPath.getValue("Program.cs").contents.shouldContain("app.MapMicrosmith();")
            byPath.getValue("Generated/Hosting/MicrosmithHostingExtensions.cs").contents
                .shouldContain("builder.Services.AddControllers();")

            val controller = byPath.getValue("Generated/Controllers/UserServiceApiControllerBase.cs")
            controller.contents.shouldContain("""[HttpGet("/users/{id}", Name = "GetUser")]""")
            controller.contents.shouldContain("""[ProducesResponseType(typeof(User), 200)]""")
            controller.contents.shouldContain("""[ProducesResponseType(typeof(Problem), 404)]""")
            controller.contents.shouldContain("protected abstract Task<GetUserResult> OnGetUserAsync")
            controller.contents.shouldContain(
                "CreateUserCreated response => Respond(" +
                    "response.Body, 201, (\"Location\", response.Location))",
            )
            controller.contents.shouldNotContain("X-Microsmith-Response-Status")
            controller.contents.shouldNotContain("sample-location")
            controller.origins shouldContain "services.UserService.rest.GetUser"
            controller.origins shouldContain "services.UserService.rest.CreateUser.body.CreateUserBody"

            byPath.getValue("Generated/Contracts/RequestModels.cs").contents
                .shouldContain("public bool IncludeDetails { get; set; } = false;")
            byPath.getValue("Generated/Contracts/RequestModels.cs").contents
                .shouldContain("[BindRequired]")
            byPath.getValue("Generated/Contracts/RequestModels.cs").contents
                .shouldContain("using System;")
            byPath.getValue("Generated/Contracts/ResponseModels.cs").contents
                .shouldContain(
                    "public sealed record CreateUserCreated(" +
                        "User Body, string? Location = null) : CreateUserResult;",
                )
        }

        "compile emits nuint defaults with a 64-bit-safe literal" {
            val requestModels = DotnetAspServiceArtifactCompiler()
                .compile(unsignedNativeIntDefaultArtifact())
                .filterIsInstance<TextFileArtifactContribution>()
                .single { it.artifactId.relativePath.toString() == "Generated/Contracts/RequestModels.cs" }
                .contents

            requestModels.shouldContain("public nuint MaxValue { get; set; } = (nuint)4294967296UL;")
        }

        "compile emits CLR usings before the contract namespace when request bindings use system types" {
            val requestModels = DotnetAspServiceArtifactCompiler()
                .compile(requestBindingTypesArtifact())
                .filterIsInstance<TextFileArtifactContribution>()
                .single { it.artifactId.relativePath.toString() == "Generated/Contracts/RequestModels.cs" }
                .contents

            requestModels.lines().take(4) shouldContainExactly listOf(
                "using System;",
                "using Microsoft.AspNetCore.Mvc.ModelBinding;",
                "",
                "namespace UserService.Api.Generated.Contracts;",
            )
            requestModels.shouldContain("public Guid ReportId { get; set; } = Guid.Empty;")
            requestModels.shouldContain("public DateOnly Since { get; set; } = DateOnly.MinValue;")
            requestModels.shouldContain("public DateTimeOffset RequestedAt { get; set; } = DateTimeOffset.UnixEpoch;")
            requestModels.shouldContain("public TimeSpan? Window { get; set; } = null;")
        }

        "compile escapes service names before embedding them in appsettings json" {
            val appSettings =
                DotnetAspServiceArtifactCompiler()
                    .compile(sampleArtifact(serviceName = "User\"Service\\Api"))
                    .filterIsInstance<TextFileArtifactContribution>()
                    .single { it.artifactId.relativePath.toString() == "appsettings.json" }
                    .contents

            appSettings.shouldContain("\"ServiceName\": \"User\\\"Service\\\\Api\"")
            appSettings.shouldNotContain("\"ServiceName\": \"User\"Service\\Api\"")
        }
    })

private fun sampleArtifact(serviceName: String = "UserService"): DotnetAspServiceArtifact {
    val userModel = sharedModel("User", "services.UserService.models.User") {
        stringField("id")
        stringField("email")
    }
    val problemModel = sharedModel("Problem", "services.UserService.models.Problem") {
        stringField("detail")
    }
    val createUserBody = inlineModel("CreateUserBody", "services.UserService.rest.CreateUser.body.CreateUserBody") {
        stringField("email")
    }

    return DotnetAspServiceArtifact(
        id = DotnetAspServiceArtifactId(solutionName = "Platform", projectName = "UserService.Api"),
        serviceName = serviceName,
        targetFrameworkMoniker = "net8.0",
        outputRoot = Path.of("dotnet", "Platform", "UserService.Api"),
        httpPort = 5000,
        httpsPort = 5001,
        contractModels = listOf(userModel, problemModel, createUserBody),
        endpoints = listOf(
            DotnetAspEndpointArtifact(
                method = "GET",
                route = "/users/{id}",
                operationName = "GetUser",
                bindings = DotnetAspEndpointBindingsArtifact(
                    path = DotnetAspRequestBindingArtifact(
                        typeName = "GetUserPath",
                        name = "GetUserPath",
                        fields = listOf(
                            DotnetAspRequestFieldArtifact(
                                name = "id",
                                type = DotnetFieldType.String,
                                optional = false,
                                defaultValue = null,
                            ),
                        ),
                        origins = setOf("services.UserService.rest.GetUser.path.GetUserPath"),
                    ),
                    query = DotnetAspRequestBindingArtifact(
                        typeName = "GetUserQuery",
                        name = "GetUserQuery",
                        fields = listOf(
                            DotnetAspRequestFieldArtifact(
                                name = "includeDetails",
                                type = DotnetFieldType.Bool,
                                optional = true,
                                defaultValue = false,
                            ),
                        ),
                        origins = setOf("services.UserService.rest.GetUser.query.GetUserQuery"),
                    ),
                    headers = DotnetAspHeadersBindingArtifact(
                        typeName = "GetUserHeaders",
                        name = "GetUserHeaders",
                        headers = listOf(
                            DotnetAspHeaderFieldArtifact(
                                name = "xCorrelationId",
                                headerName = "X-Correlation-Id",
                            ),
                        ),
                        origins = setOf("services.UserService.rest.GetUser.headers.GetUserHeaders"),
                    ),
                ),
                responses = listOf(
                    DotnetAspResponseArtifact(
                        statusCode = 200,
                        model = userModel,
                        headers = listOf(DotnetAspResponseHeaderArtifact("ETag")),
                        origins = setOf("services.UserService.rest.GetUser.responses.200"),
                    ),
                    DotnetAspResponseArtifact(
                        statusCode = 404,
                        model = problemModel,
                        headers = emptyList(),
                        origins = setOf("services.UserService.rest.GetUser.responses.404"),
                    ),
                ),
                origins = setOf("services.UserService.rest.GetUser"),
            ),
            DotnetAspEndpointArtifact(
                method = "POST",
                route = "/users",
                operationName = "CreateUser",
                bindings = DotnetAspEndpointBindingsArtifact(body = createUserBody),
                responses = listOf(
                    DotnetAspResponseArtifact(
                        statusCode = 201,
                        model = userModel,
                        headers = listOf(DotnetAspResponseHeaderArtifact("Location")),
                        origins = setOf("services.UserService.rest.CreateUser.responses.201"),
                    ),
                    DotnetAspResponseArtifact(
                        statusCode = 400,
                        model = problemModel,
                        headers = emptyList(),
                        origins = setOf("services.UserService.rest.CreateUser.responses.400"),
                    ),
                ),
                origins = setOf("services.UserService.rest.CreateUser"),
            ),
        ),
    )
}

private fun unsignedNativeIntDefaultArtifact(): DotnetAspServiceArtifact = DotnetAspServiceArtifact(
    id = DotnetAspServiceArtifactId(solutionName = "Platform", projectName = "ReportService.Api"),
    serviceName = "ReportService",
    targetFrameworkMoniker = "net8.0",
    outputRoot = Path.of("dotnet", "Platform", "ReportService.Api"),
    httpPort = 5002,
    httpsPort = 5003,
    contractModels = emptyList(),
    endpoints = listOf(
        DotnetAspEndpointArtifact(
            method = "GET",
            route = "/reports",
            operationName = "GetReport",
            bindings = DotnetAspEndpointBindingsArtifact(
                query = DotnetAspRequestBindingArtifact(
                    typeName = "GetReportQuery",
                    name = "GetReportQuery",
                    fields = listOf(
                        DotnetAspRequestFieldArtifact(
                            name = "maxValue",
                            type = DotnetFieldType.UnsignedNativeInt,
                            optional = false,
                            defaultValue = 4294967296L,
                        ),
                    ),
                    origins = setOf("services.ReportService.rest.GetReport.query.GetReportQuery"),
                ),
            ),
            responses = listOf(
                DotnetAspResponseArtifact(
                    statusCode = 200,
                    model = inlineModel(
                        "EmptyReport",
                        "services.ReportService.rest.GetReport.responses.200.EmptyReport",
                    ) {},
                    headers = emptyList(),
                    origins = setOf("services.ReportService.rest.GetReport.responses.200"),
                ),
            ),
            origins = setOf("services.ReportService.rest.GetReport"),
        ),
    ),
)

private fun requestBindingTypesArtifact(): DotnetAspServiceArtifact = DotnetAspServiceArtifact(
    id = DotnetAspServiceArtifactId(solutionName = "Platform", projectName = "UserService.Api"),
    serviceName = "UserService",
    targetFrameworkMoniker = "net8.0",
    outputRoot = Path.of("dotnet", "Platform", "UserService.Api"),
    httpPort = 5000,
    httpsPort = 5001,
    contractModels = emptyList(),
    endpoints = listOf(
        DotnetAspEndpointArtifact(
            method = "GET",
            route = "/reports/{reportId}",
            operationName = "GetReport",
            bindings = DotnetAspEndpointBindingsArtifact(
                path = DotnetAspRequestBindingArtifact(
                    typeName = "GetReportPath",
                    name = "GetReportPath",
                    fields = listOf(
                        DotnetAspRequestFieldArtifact(
                            name = "reportId",
                            type = DotnetFieldType.Guid,
                            optional = false,
                            defaultValue = null,
                        ),
                    ),
                    origins = setOf("services.UserService.rest.GetReport.path.GetReportPath"),
                ),
                query = DotnetAspRequestBindingArtifact(
                    typeName = "GetReportQuery",
                    name = "GetReportQuery",
                    fields = listOf(
                        DotnetAspRequestFieldArtifact(
                            name = "since",
                            type = DotnetFieldType.DateOnly,
                            optional = false,
                            defaultValue = null,
                        ),
                        DotnetAspRequestFieldArtifact(
                            name = "requestedAt",
                            type = DotnetFieldType.DateTimeOffset,
                            optional = false,
                            defaultValue = null,
                        ),
                        DotnetAspRequestFieldArtifact(
                            name = "window",
                            type = DotnetFieldType.TimeSpan,
                            optional = true,
                            defaultValue = null,
                        ),
                    ),
                    origins = setOf("services.UserService.rest.GetReport.query.GetReportQuery"),
                ),
            ),
            responses = listOf(
                DotnetAspResponseArtifact(
                    statusCode = 200,
                    model = inlineModel("Report", "services.UserService.rest.GetReport.responses.200.Report") {},
                    headers = emptyList(),
                    origins = setOf("services.UserService.rest.GetReport.responses.200"),
                ),
            ),
            origins = setOf("services.UserService.rest.GetReport"),
        ),
    ),
)

private fun sharedModel(
    typeName: String,
    origin: String,
    fields: MutableList<DotnetField>.() -> Unit,
): DotnetAspModelArtifact = DotnetAspModelArtifact(
    typeName = typeName,
    locality = DotnetAspModelLocality.SHARED,
    model = DotnetModel(name = typeName, fields = buildList(fields)),
    origins = setOf(origin),
)

private fun inlineModel(
    typeName: String,
    origin: String,
    fields: MutableList<DotnetField>.() -> Unit,
): DotnetAspModelArtifact = DotnetAspModelArtifact(
    typeName = typeName,
    locality = DotnetAspModelLocality.INLINE,
    model = DotnetModel(name = typeName, fields = buildList(fields)),
    origins = setOf(origin),
)

private fun MutableList<DotnetField>.stringField(name: String) {
    add(DotnetField(name = name, type = DotnetFieldType.String))
}
