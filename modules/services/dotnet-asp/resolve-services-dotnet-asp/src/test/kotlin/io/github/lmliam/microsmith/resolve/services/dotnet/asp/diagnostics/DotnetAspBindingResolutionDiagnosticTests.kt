package io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics

import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

class DotnetAspBindingResolutionDiagnosticTests :
    StringSpec({
        "maps path binding issues to diagnostics" {
            val mapper = DotnetAspResolutionDiagnosticMapper()

            listOf(
                    DotnetAspBindingResolutionIssue.PathBindingWithoutPlaceholders(
                        serviceName = "UserService",
                        operationName = "ListUsers",
                        bindingName = "ListUsersPath",
                        route = "/users",
                    ),
                    DotnetAspBindingResolutionIssue.MissingPathBinding(
                        serviceName = "UserService",
                        operationName = "GetUser",
                        route = "/users/{id}",
                        placeholders = listOf("id"),
                    ),
                    DotnetAspBindingResolutionIssue.OptionalPathBindingField(
                        serviceName = "UserService",
                        operationName = "GetUser",
                        bindingName = "GetUserPath",
                        fieldName = "id",
                    ),
                    DotnetAspBindingResolutionIssue.DefaultedPathBindingField(
                        serviceName = "UserService",
                        operationName = "GetUser",
                        bindingName = "GetUserPath",
                        fieldName = "page",
                    ),
                    DotnetAspBindingResolutionIssue.PathBindingFieldMismatch(
                        serviceName = "UserService",
                        operationName = "GetUser",
                        bindingName = "GetUserPath",
                        placeholders = listOf("id"),
                        fields = listOf("userId"),
                    ),
                    DotnetAspBindingResolutionIssue.RequestBindingReferenceField(
                        serviceName = "UserService",
                        operationName = "GetUser",
                        bindingName = "GetUserQuery",
                        fieldName = "user",
                        targetName = "User",
                    ),
                )
                .map(mapper::map) shouldContainExactly
                listOf(
                    ResolutionDiagnostic(
                        code = "dotnet.asp.path-binding-without-placeholders",
                        message =
                            "ASP.NET endpoint 'ListUsers' in service " +
                                "'UserService' declares path binding " +
                                "'ListUsersPath' but route '/users' " +
                                "has no placeholders.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.asp.path-binding-required",
                        message =
                            "ASP.NET endpoint 'GetUser' in service " +
                                "'UserService' must declare a path " +
                                "binding for route '/users/{id}'.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.asp.path-binding-field-optional",
                        message =
                            "ASP.NET path binding 'GetUserPath' " +
                                "field 'id' in operation 'GetUser' " +
                                "cannot be optional.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.asp.path-binding-field-defaulted",
                        message =
                            "ASP.NET path binding 'GetUserPath' " +
                                "field 'page' in operation 'GetUser' " +
                                "cannot declare a default value.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.asp.path-binding-field-mismatch",
                        message =
                            "ASP.NET path binding 'GetUserPath' in " +
                                "operation 'GetUser' must match route " +
                                "placeholders id, but declared userId.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.asp.request-binding-reference-field",
                        message =
                            "ASP.NET request binding 'GetUserQuery' " +
                                "field 'user' in operation 'GetUser' " +
                                "cannot reference shared model 'User'. " +
                                "Transport bindings must declare " +
                                "scalar fields.",
                    ),
                )
        }

        "maps structured model reference sources to diagnostics" {
            val mapper = DotnetAspResolutionDiagnosticMapper()

            listOf(
                    DotnetAspBindingResolutionIssue.UnknownSharedModelReference(
                        serviceName = "UserService",
                        operationName = "CreateUser",
                        source = DotnetAspBindingResolutionIssue.ModelReferenceSource.RequestBody,
                        targetName = "MissingBody",
                    ),
                    DotnetAspBindingResolutionIssue.UnknownSharedModelReference(
                        serviceName = "UserService",
                        operationName = "CreateUser",
                        source = DotnetAspBindingResolutionIssue.ModelReferenceSource.Response(400),
                        targetName = "Problem",
                    ),
                    DotnetAspBindingResolutionIssue.UnknownSharedModelReference(
                        serviceName = "UserService",
                        operationName = "CreateUser",
                        source =
                            DotnetAspBindingResolutionIssue.ModelReferenceSource.InlineModelField(
                                modelName = "CreateUserBody",
                                fieldName = "manager",
                            ),
                        targetName = "MissingUser",
                    ),
                )
                .map(mapper::map) shouldContainExactly
                listOf(
                    ResolutionDiagnostic(
                        code = "dotnet.asp.unknown-shared-model-reference",
                        message =
                            "ASP.NET request body in operation " +
                                "'CreateUser' for service " +
                                "'UserService' references unknown " +
                                "shared model 'MissingBody'.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.asp.unknown-shared-model-reference",
                        message =
                            "ASP.NET response 400 in operation " +
                                "'CreateUser' for service " +
                                "'UserService' references unknown " +
                                "shared model 'Problem'.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.asp.unknown-shared-model-reference",
                        message =
                            "ASP.NET inline model 'CreateUserBody' " +
                                "field 'manager' in operation " +
                                "'CreateUser' for service " +
                                "'UserService' references unknown " +
                                "shared model 'MissingUser'.",
                    ),
                )
        }
    })
