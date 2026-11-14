package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspDefaultValue
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspRequestBindingScope
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.service.asp
import io.github.lmliam.microsmith.dsl.services.dotnet.dotnet
import io.github.lmliam.microsmith.dsl.services.services
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspBindingResolutionIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.math.BigInteger

private fun resolveQuery(
    query: DotnetAspRequestBindingScope.() -> Unit,
): arrow.core.EitherNel<DotnetResolutionIssue, DotnetAspWorkspace> {
    val builder = MicrosmithBuilder()
    builder.services {
        dotnet {
            target(NET8)
            solutions { "Platform" {} }
        }
        "ReportService" {
            dotnet {
                solution("Platform")
                project("ReportService.Api")
                models { "Report" { string("id") } }
                asp {
                    rest {
                        "/reports" {
                            get("GetReport") {
                                query("GetReportQuery", query)
                                responses { ok("Report") }
                            }
                        }
                    }
                }
            }
        }
    }
    return DotnetAspWorkspaceResolver().resolve(requireNotNull(builder.model.get<ServicesExtension>()))
}

private fun resolveQueryIssues(query: DotnetAspRequestBindingScope.() -> Unit): List<DotnetResolutionIssue> =
    resolveQuery(query).fold(ifLeft = { it.toList() }, ifRight = { error("Expected ASP.NET resolution failure") })

private fun resolvedDefault(query: DotnetAspRequestBindingScope.() -> Unit): DotnetAspDefaultValue? =
    resolveQuery(query)
        .fold(ifLeft = { error("Expected success but got ${it.toList()}") }, ifRight = { it })
        .servicesByName
        .getValue("ReportService")
        .rest
        .endpoints
        .single()
        .bindings
        .query!!
        .fields
        .single()
        .defaultValue

class DotnetAspIntegerDefaultResolutionTests :
    StringSpec(
        {
            "integer default beyond the range of its type is reported" {
                resolveQueryIssues { int("limit") { default(99_999_999_999L) } } shouldContainExactly
                    listOf(
                        DotnetAspBindingResolutionIssue.IntegerDefaultOutOfRange(
                            serviceName = "ReportService",
                            operationName = "GetReport",
                            bindingName = "GetReportQuery",
                            fieldName = "limit",
                            csharpTypeName = "int",
                            value = "99999999999",
                        ),
                    )
            }

            "negative default for an unsigned field is reported" {
                resolveQueryIssues { uint("limit") { default(-1) } } shouldContainExactly
                    listOf(
                        DotnetAspBindingResolutionIssue.IntegerDefaultOutOfRange(
                            serviceName = "ReportService",
                            operationName = "GetReport",
                            bindingName = "GetReportQuery",
                            fieldName = "limit",
                            csharpTypeName = "uint",
                            value = "-1",
                        ),
                    )
            }

            "fractional default for an integer field is reported" {
                resolveQueryIssues { int("limit") { default(1.5) } } shouldContainExactly
                    listOf(
                        DotnetAspBindingResolutionIssue.IntegerDefaultNotIntegral(
                            serviceName = "ReportService",
                            operationName = "GetReport",
                            bindingName = "GetReportQuery",
                            fieldName = "limit",
                            csharpTypeName = "int",
                            value = "1.5",
                        ),
                    )
            }

            "whole floating point default for an integer field resolves to an exact integer" {
                resolvedDefault { int("limit") { default(5.0) } } shouldBe
                    DotnetAspDefaultValue.NumericValue(BigInteger.valueOf(5))
            }

            "default at the upper bound of a native unsigned integer resolves" {
                resolvedDefault { nuint("limit") { default(BigInteger("18446744073709551615")) } } shouldBe
                    DotnetAspDefaultValue.NumericValue(BigInteger("18446744073709551615"))
            }

            "floating point field keeps its fractional default" {
                resolvedDefault { double("ratio") { default(1.5) } } shouldBe DotnetAspDefaultValue.NumericValue(1.5)
            }
        },
    )
