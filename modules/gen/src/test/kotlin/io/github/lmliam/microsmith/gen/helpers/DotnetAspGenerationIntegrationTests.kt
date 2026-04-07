package io.github.lmliam.microsmith.gen.helpers

import io.github.lmliam.microsmith.dsl.core.microsmith
import io.github.lmliam.microsmith.dsl.services.core.services
import io.github.lmliam.microsmith.dsl.services.dotnet.core.dotnet
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.asp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.io.path.writeText

class DotnetAspGenerationIntegrationTests :
    StringSpec({
        "generateTo emits the ASP.NET scaffold and endpoint extension surface" {
            val outputDir = Files.createTempDirectory("microsmith-dotnet-asp-output-")
            val model =
                microsmith {
                    services {
                        dotnet {
                            target(NET8)
                            solutions {
                                "Platform" {}
                            }
                        }

                        "UserService" {
                            dotnet {
                                solution("Platform")
                                project("UserService.Api")
                                models {
                                    "User" {
                                        string("id")
                                        string("email")
                                    }

                                    "Problem" {
                                        string("message")
                                    }
                                }

                                asp {
                                    rest {
                                        "/users" {
                                            get("/{id}", "GetUser") {
                                                path("GetUserPath") {
                                                    string("id")
                                                }

                                                responses {
                                                    ok("User")
                                                    notFound("Problem") {
                                                        headers {
                                                            header("X-Trace-Id")
                                                        }
                                                    }
                                                }
                                            }

                                            post("CreateUser") {
                                                query("CreateUserQuery") {
                                                    bool("dryRun") {
                                                        optional()
                                                        default(false)
                                                    }
                                                }

                                                body("Body") {
                                                    string("email")
                                                }

                                                responses {
                                                    created("User") {
                                                        headers {
                                                            header("Location")
                                                        }
                                                    }
                                                    badRequest("Problem") {
                                                        model {
                                                            string("message")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            model.generateTo(outputDir)

            val projectRoot = outputDir.resolve("dotnet/Platform/UserService.Api")
            Files.exists(projectRoot.resolve("UserService.Api.csproj")) shouldBe true
            Files.exists(projectRoot.resolve("Program.cs")) shouldBe false
            Files.exists(projectRoot.resolve("appsettings.json")) shouldBe true
            Files.exists(projectRoot.resolve("Properties/launchSettings.json")) shouldBe true
            Files.exists(projectRoot.resolve("Generated/Hosting/MicrosmithHostingExtensions.cs")) shouldBe true
            Files.exists(projectRoot.resolve("Generated/Contracts/ServiceModels.cs")) shouldBe true
            Files.exists(projectRoot.resolve("Generated/Contracts/RequestModels.cs")) shouldBe true
            Files.exists(projectRoot.resolve("Generated/Contracts/ResponseModels.cs")) shouldBe true
            Files.exists(
                projectRoot.resolve("Generated/Controllers/UserServiceApiControllerBase.cs"),
            ) shouldBe true
            Files.exists(
                projectRoot.resolve("Controllers/UserServiceApiController.cs"),
            ) shouldBe false
            projectRoot.resolve("Generated/Hosting/MicrosmithHostingExtensions.cs").readText()
                .shouldContain("Generated by Microsmith")
            projectRoot.resolve("Generated/Hosting/MicrosmithHostingExtensions.cs").readText()
                .shouldContain("AddMicrosmith")
            projectRoot.resolve("Generated/Hosting/MicrosmithHostingExtensions.cs").readText()
                .shouldContain("MapMicrosmith")
            projectRoot.resolve("Generated/Controllers/UserServiceApiControllerBase.cs").readText()
                .shouldContain("protected abstract Task<GetUserResult> OnGetUserAsync(")
            projectRoot.resolve("Generated/Contracts/RequestModels.cs").readText()
                .shouldContain("public bool DryRun { get; set; } = false;")
        }

        "generateTo overwrites generated ASP.NET endpoint files on rerun" {
            val outputDir = Files.createTempDirectory("microsmith-dotnet-asp-rerun-")
            val initialModel =
                microsmith {
                    services {
                        dotnet {
                            target(NET8)
                            solutions {
                                "Platform" {}
                            }
                        }

                        "UserService" {
                            dotnet {
                                solution("Platform")
                                project("UserService.Api")
                                models {
                                    "User" {
                                        string("id")
                                    }
                                }

                                asp {
                                    rest {
                                        "/users" {
                                            get("/{id}", "GetUser") {
                                                path("GetUserPath") {
                                                    string("id")
                                                }

                                                responses {
                                                    ok("User")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            initialModel.generateTo(outputDir)

            val controllerBaseFile =
                outputDir
                    .resolve("dotnet/Platform/UserService.Api")
                    .resolve("Generated/Controllers/UserServiceApiControllerBase.cs")
            val requestModelsFile =
                outputDir
                    .resolve("dotnet/Platform/UserService.Api")
                    .resolve("Generated/Contracts/RequestModels.cs")
            controllerBaseFile.writeText("stale")
            requestModelsFile.writeText("stale")

            val updatedModel =
                microsmith {
                    services {
                        dotnet {
                            target(NET8)
                            solutions {
                                "Platform" {}
                            }
                        }

                        "UserService" {
                            dotnet {
                                solution("Platform")
                                project("UserService.Api")
                                models {
                                    "User" {
                                        string("id")
                                    }
                                }

                                asp {}
                            }
                        }
                    }
                }

            updatedModel.generateTo(outputDir)

            controllerBaseFile.readText()
                .shouldContain("public abstract class UserServiceApiControllerBase : MicrosmithControllerBase;")
            controllerBaseFile.readText().shouldNotContain("stale")
            controllerBaseFile.readText().shouldNotContain("OnGetUserAsync")
            requestModelsFile.readText().shouldContain("namespace UserService.Api.Generated.Contracts;")
            requestModelsFile.readText().shouldNotContain("stale")
            requestModelsFile.readText().shouldNotContain("GetUserPath")
        }
    })
