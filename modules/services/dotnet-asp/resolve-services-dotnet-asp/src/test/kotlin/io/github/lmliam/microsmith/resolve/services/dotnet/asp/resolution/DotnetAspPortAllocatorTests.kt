package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import arrow.core.EitherNel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspPorts
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private fun request(project: String, http: Int? = null, https: Int? = null) =
    DotnetAspPortRequest(
        serviceName = project.substringBefore('.'),
        solutionName = "Platform",
        projectName = project,
        http = http,
        https = https,
    )

private fun EitherNel<DotnetAspResolutionIssue, Map<String, ResolvedDotnetAspPorts>>.ports():
    Map<String, ResolvedDotnetAspPorts> =
    fold(ifLeft = { error("Expected success but got ${it.toList()}") }, ifRight = { it })

class DotnetAspPortAllocatorTests :
    StringSpec(
        {
            "default ports of a service do not change when an unrelated service is added" {
                val alone = DotnetAspPortAllocator.allocate(listOf(request("User.Api"))).ports().getValue("User")
                val withNeighbour =
                    DotnetAspPortAllocator.allocate(listOf(request("Admin.Api"), request("User.Api")))
                        .ports()
                        .getValue("User")

                withNeighbour shouldBe alone
            }

            "default ports follow the stable hash of solution and project" {
                val ports = DotnetAspPortAllocator.allocate(listOf(request("User.Api"))).ports().getValue("User")

                ports shouldBe ResolvedDotnetAspPorts(http = 7_470, https = 7_471)
            }

            "colliding default ports move the later service to the next free slot" {
                val ports =
                    DotnetAspPortAllocator.allocate(listOf(request("Collision210.Api"), request("Collision9.Api")))
                        .ports()

                ports.getValue("Collision210") shouldBe ResolvedDotnetAspPorts(http = 17_190, https = 17_191)
                ports.getValue("Collision9") shouldBe ResolvedDotnetAspPorts(http = 17_200, https = 17_201)
            }

            "colliding default ports in the last slot wrap around to the first slot" {
                val ports =
                    DotnetAspPortAllocator.allocate(listOf(request("Edge2785.Api"), request("Edge433.Api"))).ports()

                ports.getValue("Edge2785") shouldBe ResolvedDotnetAspPorts(http = 19_990, https = 19_991)
                ports.getValue("Edge433") shouldBe ResolvedDotnetAspPorts(http = 5_000, https = 5_001)
            }

            "colliding default ports resolve the same way for either declaration order" {
                val forward =
                    DotnetAspPortAllocator.allocate(listOf(request("Collision9.Api"), request("Collision210.Api")))
                val reversed =
                    DotnetAspPortAllocator.allocate(listOf(request("Collision210.Api"), request("Collision9.Api")))

                forward.ports() shouldBe reversed.ports()
            }

            "explicit ports win over a colliding default allocation" {
                val ports =
                    DotnetAspPortAllocator.allocate(
                            listOf(request("Default.Api"), request("Pinned.Api", http = 12_210)),
                        )
                        .ports()

                ports.getValue("Pinned") shouldBe ResolvedDotnetAspPorts(http = 12_210, https = 12_211)
                ports.getValue("Default") shouldBe ResolvedDotnetAspPorts(http = 12_220, https = 12_221)
            }

            "a default allocation avoids the https port of an explicit service" {
                val ports =
                    DotnetAspPortAllocator.allocate(
                            listOf(request("Default.Api"), request("Pinned.Api", http = 12_200, https = 12_210)),
                        )
                        .ports()

                ports.getValue("Default") shouldBe ResolvedDotnetAspPorts(http = 12_220, https = 12_221)
            }

            "a default allocation reports when every default slot is taken" {
                val pinned = (0 until 1_500).map { slot -> request("Pinned$slot.Api", http = 5_000 + slot * 10) }

                DotnetAspPortAllocator.allocate(pinned + request("Default.Api"))
                    .fold(
                        ifLeft = { it.toList() },
                        ifRight = { error("Expected exhaustion") },
                    ) shouldContainExactly listOf(DotnetAspResolutionIssue.NoFreeDefaultPorts("Default"))
            }
        },
    )
