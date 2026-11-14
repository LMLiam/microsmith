package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import arrow.core.EitherNel
import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.service.asp
import io.github.lmliam.microsmith.dsl.services.dotnet.dotnet
import io.github.lmliam.microsmith.dsl.services.services
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspPorts
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue.PortKind
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

private data class PortedService(val name: String, val http: Int? = null, val https: Int? = null) {
    val project: String = "$name.Api"
}

private fun resolvePorts(vararg services: PortedService): EitherNel<DotnetResolutionIssue, DotnetAspWorkspace> {
    val builder = MicrosmithBuilder()
    builder.services {
        dotnet {
            target(NET10)
            solutions { "Platform" {} }
        }
        services.forEach { portedService ->
            portedService.name.invoke {
                dotnet {
                    solution("Platform")
                    project(portedService.project)
                    asp {
                        if (portedService.http != null || portedService.https != null) {
                            ports {
                                portedService.http?.let { http(it) }
                                portedService.https?.let { https(it) }
                            }
                        }
                    }
                }
            }
        }
    }
    return DotnetAspWorkspaceResolver().resolve(requireNotNull(builder.model.get<ServicesExtension>()))
}

private fun EitherNel<DotnetResolutionIssue, DotnetAspWorkspace>.issues(): List<DotnetResolutionIssue> =
    fold(ifLeft = { it.toList() }, ifRight = { error("Expected ASP.NET resolution failure") })

private fun EitherNel<DotnetResolutionIssue, DotnetAspWorkspace>.workspace(): DotnetAspWorkspace =
    fold(ifLeft = { error("Expected success but got ${it.toList()}") }, ifRight = { it })

private fun DotnetAspWorkspace.portsOf(name: String): ResolvedDotnetAspPorts =
    requireNotNull(servicesByName[name]).ports

class DotnetAspPortResolutionTests :
    StringSpec(
        {
            "explicit https port derives the http port one below it" {
                val workspace = resolvePorts(PortedService("UserService", https = 7443)).workspace()

                workspace.portsOf("UserService") shouldBe ResolvedDotnetAspPorts(http = 7442, https = 7443)
            }

            "explicit http port derives the https port one above it" {
                val workspace = resolvePorts(PortedService("UserService", http = 7000)).workspace()

                workspace.portsOf("UserService") shouldBe ResolvedDotnetAspPorts(http = 7000, https = 7001)
            }

            "explicit http port 65535 reports the derived https port out of range" {
                resolvePorts(PortedService("UserService", http = 65_535)).issues() shouldContainExactly
                    listOf(DotnetAspResolutionIssue.PortOutOfRange("UserService", PortKind.HTTPS, 65_536))
            }

            "explicit https port 1 reports the derived http port out of range" {
                resolvePorts(PortedService("UserService", https = 1)).issues() shouldContainExactly
                    listOf(DotnetAspResolutionIssue.PortOutOfRange("UserService", PortKind.HTTP, 0))
            }

            "explicit ports shared by two services are reported with their owners" {
                resolvePorts(
                        PortedService("AdminService", http = 7000, https = 7443),
                        PortedService("UserService", http = 7000, https = 7444),
                    )
                    .issues() shouldContainExactly
                    listOf(DotnetAspResolutionIssue.LaunchPortCollision(7000, listOf("AdminService", "UserService")))
            }

            "every explicit collision and out-of-range port is reported together" {
                resolvePorts(
                        PortedService("AdminService", http = 7000, https = 7443),
                        PortedService("UserService", http = 7000, https = 7443),
                        PortedService("BillingService", http = 65_535),
                    )
                    .issues() shouldContainExactlyInAnyOrder
                    listOf(
                        DotnetAspResolutionIssue.LaunchPortCollision(7000, listOf("AdminService", "UserService")),
                        DotnetAspResolutionIssue.LaunchPortCollision(7443, listOf("AdminService", "UserService")),
                        DotnetAspResolutionIssue.PortOutOfRange("BillingService", PortKind.HTTPS, 65_536),
                    )
            }

            "a derived port colliding with another explicit port is reported" {
                resolvePorts(
                        PortedService("AdminService", http = 7000),
                        PortedService("UserService", http = 7001, https = 7443),
                    )
                    .issues() shouldContainExactly
                    listOf(DotnetAspResolutionIssue.LaunchPortCollision(7001, listOf("AdminService", "UserService")))
            }

            "default ports of many services are all distinct" {
                val services = (1..400).map { PortedService("Service$it") }.toTypedArray()

                val ports = resolvePorts(*services).workspace().servicesByName.values.map { it.ports }

                ports.flatMap { listOf(it.http, it.https) }.toSet().size shouldBe 800
            }
        },
    )
