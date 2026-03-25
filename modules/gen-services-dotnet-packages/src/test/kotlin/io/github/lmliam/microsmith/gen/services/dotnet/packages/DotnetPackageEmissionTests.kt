package io.github.lmliam.microsmith.gen.services.dotnet.packages

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.core.services
import io.github.lmliam.microsmith.dsl.services.dotnet.core.dotnet
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.packages
import io.github.lmliam.microsmith.dsl.services.dotnet.core.solution.packages
import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.github.lmliam.microsmith.gen.services.ServiceEmitterRegistry
import io.github.lmliam.microsmith.gen.services.ServicesGenerationService
import io.github.lmliam.microsmith.gen.services.dotnet.packages.emission.DotnetPackageReferencesEmitter
import io.github.lmliam.microsmith.gen.services.dotnet.packages.emission.DotnetPackageVersionsEmitter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.io.path.Path

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

class DotnetPackageEmissionTests :
    StringSpec({
        "generate emits central package props and project package references" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET9)
                    solutions {
                        "Platform" {
                            packages {
                                "Serilog" {
                                    version("9.0.0")
                                    +"AspNetCore"
                                    "Settings.Configuration" {
                                        version("9.0.1")
                                    }
                                }
                            }
                        }
                    }
                }

                "UserService" {
                    dotnet {
                        solution("Platform")
                        project("UserService.Api")
                        packages {
                            "Serilog" {
                                +"AspNetCore"
                                +"Settings.Configuration"
                            }
                        }
                    }
                }
            }

            val emitters = listOf(DotnetPackageVersionsEmitter(), DotnetPackageReferencesEmitter())
            val generationService =
                ServicesGenerationService(
                    emitterRegistry = ServiceEmitterRegistry(emitters),
                )
            val space = DirectorySpace.from(Files.createTempDirectory("microsmith-dotnet-packages-"))

            val generated = generationService.generate(builder.requireServicesExtension(), space)

            generated.size shouldBe 2
            generated[0].relativePath shouldBe Path("Directory.Packages.props")
            generated[0].outputRoot shouldBe Path("dotnet", "Platform")
            generated[0].contents.toString(StandardCharsets.UTF_8) shouldBe
                """
                <Project>
                  <PropertyGroup>
                    <ManagePackageVersionsCentrally>true</ManagePackageVersionsCentrally>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageVersion Include="Serilog.AspNetCore" Version="9.0.0" />
                    <PackageVersion Include="Serilog.Settings.Configuration" Version="9.0.1" />
                  </ItemGroup>
                </Project>
                """.trimIndent() + "\n"

            generated[1].relativePath shouldBe Path("PackageReferences.props")
            generated[1].outputRoot shouldBe Path("dotnet", "Platform", "UserService.Api")
            generated[1].contents.toString(StandardCharsets.UTF_8) shouldBe
                """
                <Project>
                  <ItemGroup>
                    <PackageReference Include="Serilog.AspNetCore" />
                    <PackageReference Include="Serilog.Settings.Configuration" />
                  </ItemGroup>
                </Project>
                """.trimIndent() + "\n"
        }
    })
