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
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path

class DotnetAspServiceArtifactCompilerTests :
    StringSpec({
        "compile emits the generated ASP.NET project layout" {
            val artifact = sampleArtifact()

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
            msbuild.origins shouldBe setOf("services.UserService")

            textFiles.map { it.artifactId.relativePath.toString() } shouldContainExactlyInAnyOrder listOf(
                "Program.cs",
                "appsettings.json",
                "Properties/launchSettings.json",
                "Controllers/UserServiceController.cs",
                "Models/User.cs",
                "Models/Problem.cs",
                "Models/CreateUserBody.cs",
                "Bindings/GetUserPath.cs",
                "Bindings/GetUserQuery.cs",
                "Bindings/GetUserHeaders.cs",
                "Generated/MicrosmithRequestParser.cs",
            )
            textFiles.forEach {
                it.artifactId.outputRoot shouldBe Path.of("dotnet", "Platform", "UserService.Api")
            }

            textFiles.single { it.artifactId.relativePath.toString() == "Program.cs" }.contents
                .shouldContain("AddControllers")
            textFiles.single { it.artifactId.relativePath.toString() == "Program.cs" }.contents
                .shouldContain("public partial class Program { }")
            textFiles.single { it.artifactId.relativePath.toString() == "Program.cs" }.contents
                .shouldContain("StatusCodes.Status400BadRequest")

            val controller = textFiles.single { it.artifactId.relativePath.toString() == "Controllers/UserServiceController.cs" }
            controller.contents.shouldContain("""[HttpGet("/users/{id}", Name = "GetUser")]""")
            controller.contents.shouldContain("""[HttpPost("/users", Name = "CreateUser")]""")
            controller.contents.shouldContain("MicrosmithRequestParser.ReadRouteValue")
            controller.contents.shouldContain("""Response.Headers["Location"] = "sample-location";""")
            controller.origins shouldContain "services.UserService.rest.GetUser"
            controller.origins shouldContain "services.UserService.rest.CreateUser.body.CreateUserBody"
            controller.origins shouldContain "services.UserService.rest.GetUser.responses.200"

            textFiles.single { it.artifactId.relativePath.toString() == "Models/CreateUserBody.cs" }.contents
                .shouldContain("public sealed class CreateUserBody")
            textFiles.single { it.artifactId.relativePath.toString() == "Controllers/UserServiceController.cs" }.contents
                .shouldContain("?? false")
            textFiles.single { it.artifactId.relativePath.toString() == "Generated/MicrosmithRequestParser.cs" }.contents
                .shouldContain("internal static bool? OptionalBool")
            textFiles.single { it.artifactId.relativePath.toString() == "appsettings.json" }.contents
                .shouldContain("\"ServiceName\": \"UserService\"")
            textFiles.single { it.artifactId.relativePath.toString() == "Properties/launchSettings.json" }.contents
                .shouldContain("http://localhost:5000;https://localhost:5001")
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

        "compile emits typed request bindings for supported scalar inputs" {
            val artifact = typedBindingArtifact()

            val textFiles = DotnetAspServiceArtifactCompiler()
                .compile(artifact)
                .filterIsInstance<TextFileArtifactContribution>()

            val controller = textFiles.single { it.artifactId.relativePath.toString() == "Controllers/ReportServiceController.cs" }.contents
            val parser = textFiles.single { it.artifactId.relativePath.toString() == "Generated/MicrosmithRequestParser.cs" }.contents

            controller.shouldContain("""MicrosmithRequestParser.RequireGuid(MicrosmithRequestParser.ReadRouteValue(RouteData.Values, "reportId"), "path.reportId")""")
            controller.shouldContain("""MicrosmithRequestParser.RequireInt(MicrosmithRequestParser.ReadQueryValue(Request.Query, "days"), "query.days")""")
            controller.shouldContain("""MicrosmithRequestParser.RequireDateOnly(MicrosmithRequestParser.ReadQueryValue(Request.Query, "since"), "query.since")""")
            controller.shouldContain("""MicrosmithRequestParser.RequireDateTimeOffset(MicrosmithRequestParser.ReadQueryValue(Request.Query, "requestedAt"), "query.requestedAt")""")
            controller.shouldContain("""(MicrosmithRequestParser.OptionalDecimal(MicrosmithRequestParser.ReadQueryValue(Request.Query, "threshold"), "query.threshold") ?? 1.5M)""")
            controller.shouldContain("""MicrosmithRequestParser.OptionalTimeSpan(MicrosmithRequestParser.ReadQueryValue(Request.Query, "window"), "query.window")""")

            parser.shouldContain("internal static Guid RequireGuid")
            parser.shouldContain("internal static DateOnly RequireDateOnly")
            parser.shouldContain("internal static DateTimeOffset RequireDateTimeOffset")
            parser.shouldContain("internal static TimeSpan? OptionalTimeSpan")
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
                                name = "correlationId",
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

private fun typedBindingArtifact(): DotnetAspServiceArtifact {
    val reportModel = sharedModel("Report", "services.ReportService.models.Report") {
        stringField("id")
        stringField("title")
    }

    return DotnetAspServiceArtifact(
        id = DotnetAspServiceArtifactId(solutionName = "Platform", projectName = "ReportService.Api"),
        serviceName = "ReportService",
        targetFrameworkMoniker = "net8.0",
        outputRoot = Path.of("dotnet", "Platform", "ReportService.Api"),
        httpPort = 5002,
        httpsPort = 5003,
        contractModels = listOf(reportModel),
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
                        origins = setOf("services.ReportService.rest.GetReport.path.GetReportPath"),
                    ),
                    query = DotnetAspRequestBindingArtifact(
                        typeName = "GetReportQuery",
                        name = "GetReportQuery",
                        fields = listOf(
                            DotnetAspRequestFieldArtifact(
                                name = "days",
                                type = DotnetFieldType.Int,
                                optional = false,
                                defaultValue = null,
                            ),
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
                                name = "threshold",
                                type = DotnetFieldType.Decimal,
                                optional = true,
                                defaultValue = 1.5,
                            ),
                            DotnetAspRequestFieldArtifact(
                                name = "window",
                                type = DotnetFieldType.TimeSpan,
                                optional = true,
                                defaultValue = null,
                            ),
                        ),
                        origins = setOf("services.ReportService.rest.GetReport.query.GetReportQuery"),
                    ),
                ),
                responses = listOf(
                    DotnetAspResponseArtifact(
                        statusCode = 200,
                        model = reportModel,
                        headers = emptyList(),
                        origins = setOf("services.ReportService.rest.GetReport.responses.200"),
                    ),
                ),
                origins = setOf("services.ReportService.rest.GetReport"),
            ),
        ),
    )
}

private fun sharedModel(
    name: String,
    origin: String,
    fields: MutableList<DotnetField>.() -> Unit,
): DotnetAspModelArtifact = DotnetAspModelArtifact(
    typeName = name,
    locality = DotnetAspModelLocality.SHARED,
    model = DotnetModel(name = name, fields = buildList(fields)),
    origins = setOf(origin),
)

private fun inlineModel(
    name: String,
    origin: String,
    fields: MutableList<DotnetField>.() -> Unit,
): DotnetAspModelArtifact = DotnetAspModelArtifact(
    typeName = name,
    locality = DotnetAspModelLocality.INLINE,
    model = DotnetModel(name = name, fields = buildList(fields)),
    origins = setOf(origin),
)

private fun MutableList<DotnetField>.stringField(name: String) {
    add(DotnetField(name = name, type = DotnetFieldType.String))
}
