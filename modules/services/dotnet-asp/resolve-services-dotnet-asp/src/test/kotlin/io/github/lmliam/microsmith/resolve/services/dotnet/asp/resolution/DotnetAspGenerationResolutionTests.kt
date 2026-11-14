package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.service.DotnetAspRestScope
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.service.asp
import io.github.lmliam.microsmith.dsl.services.dotnet.dotnet
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModelsScope
import io.github.lmliam.microsmith.dsl.services.services
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionIssueDiagnosticMapper
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionDiagnosticMapper
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private fun resolveDiagnostics(
    project: String = "UserService.Api",
    models: DotnetModelsScope.() -> Unit = { "User" { string("id") } },
    rest: DotnetAspRestScope.() -> Unit,
): List<ResolutionDiagnostic> {
    val builder = MicrosmithBuilder()
    builder.services {
        dotnet {
            target(NET8)
            solutions { "Platform" {} }
        }
        "UserService" {
            dotnet {
                solution("Platform")
                project(project)
                models(models)
                asp { rest(rest) }
            }
        }
    }
    val mapper: ResolutionIssueDiagnosticMapper<DotnetAspResolutionIssue> = DotnetAspResolutionDiagnosticMapper()
    return DotnetAspWorkspaceResolver()
        .resolve(requireNotNull(builder.model.get<ServicesExtension>()))
        .fold(
            ifLeft = { issues -> issues.toList().map { mapper.map(it.asAspIssue()) } },
            ifRight = { emptyList() },
        )
}

private fun DotnetResolutionIssue.asAspIssue(): DotnetAspResolutionIssue = this as DotnetAspResolutionIssue

private fun List<ResolutionDiagnostic>.codes(): List<String> = map { it.code }

class DotnetAspGenerationResolutionTests :
    StringSpec({
        "response header named body collides with the generated result body property" {
            val diagnostics = resolveDiagnostics {
                "/users" { get("GetUser") { responses { ok("User") { headers { header("body") } } } } }
            }

            diagnostics.codes() shouldContainExactly listOf("dotnet.asp.response-header-body-collision")
            diagnostics.single().message shouldContain "header 'body'"
        }

        "headers that both derive the body property are reported only as body collisions" {
            val diagnostics = resolveDiagnostics {
                "/users" {
                    get("GetUser") {
                        responses {
                            ok("User") {
                                headers {
                                    header("body")
                                    header("-body")
                                }
                            }
                        }
                    }
                }
            }

            diagnostics.codes() shouldContainExactly
                listOf("dotnet.asp.response-header-body-collision", "dotnet.asp.response-header-body-collision")
        }

        "response headers that derive the same property name are reported together" {
            val diagnostics = resolveDiagnostics {
                "/users" {
                    get("GetUser") {
                        responses {
                            ok("User") {
                                headers {
                                    header("X-Request-Id")
                                    header("x_request_id")
                                }
                            }
                        }
                    }
                }
            }

            diagnostics.codes() shouldContainExactly listOf("dotnet.asp.response-header-collision")
            diagnostics.single().message shouldContain "XRequestId"
        }

        "response header without letters or digits cannot derive a property name" {
            val diagnostics = resolveDiagnostics {
                "/users" { get("GetUser") { responses { ok("User") { headers { header("---") } } } } }
            }

            diagnostics.codes() shouldContainExactly listOf("dotnet.asp.response-header-invalid")
        }

        "no content response with body fields is reported" {
            val diagnostics = resolveDiagnostics {
                "/users" { delete("DeleteUser") { responses { noContent("User") } } }
            }

            diagnostics.codes() shouldContainExactly listOf("dotnet.asp.no-content-response-body")
            diagnostics.single().message shouldContain "DeleteUser"
        }

        "no content response without body fields resolves" {
            val diagnostics =
                resolveDiagnostics(models = { "Empty" {} }) {
                    "/users" { delete("DeleteUser") { responses { noContent("Empty") } } }
                }

            diagnostics shouldBe emptyList()
        }

        "project that derives the shared controller base type name is reported" {
            val diagnostics = resolveDiagnostics(project = "Microsmith") {}

            diagnostics.codes() shouldContainExactly listOf("dotnet.asp.controller-base-type-collision")
        }

        "shared model named like a generated result type is reported" {
            val diagnostics =
                resolveDiagnostics(models = { "GetUserResult" { string("id") } }) {
                    "/users" { get("GetUser") { responses { ok("GetUserResult") } } }
                }

            diagnostics.codes() shouldContainExactly listOf("dotnet.asp.contract-type-collision")
            diagnostics.single().message shouldContain "GetUserResult"
        }

        "shared model named like a generated result variant is reported" {
            val diagnostics =
                resolveDiagnostics(models = { "GetUserOk" { string("id") } }) {
                    "/users" { get("GetUser") { responses { ok("GetUserOk") } } }
                }

            diagnostics.codes() shouldContainExactly listOf("dotnet.asp.contract-type-collision")
        }

        "every generation conflict is reported together" {
            val diagnostics =
                resolveDiagnostics(project = "Microsmith") {
                    "/users" {
                        get("GetUser") { responses { ok("User") { headers { header("body") } } } }
                        delete("DeleteUser") { responses { noContent("User") } }
                    }
                }

            diagnostics.codes() shouldContainExactlyInAnyOrder
                listOf(
                    "dotnet.asp.controller-base-type-collision",
                    "dotnet.asp.response-header-body-collision",
                    "dotnet.asp.no-content-response-body",
                )
        }
    })
