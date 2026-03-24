package io.github.lmliam.microsmith.dsl.services.dotnet.core

import io.github.lmliam.microsmith.dsl.core.MergeableExtension
import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.core.services
import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget.NET8
import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget.NET9
import io.github.lmliam.microsmith.dsl.services.dotnet.core.defaults.DotnetDefaultsExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.DotnetServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.solution.DotnetSolutionContext
import io.github.lmliam.microsmith.dsl.services.dotnet.core.solution.DotnetSolutionScope
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private data class TestSolutionExtension(
    val values: List<String>,
) : MicrosmithExtension, MergeableExtension<TestSolutionExtension> {
    override fun merge(other: TestSolutionExtension) = TestSolutionExtension(values + other.values)
}

private fun DotnetSolutionScope.marker(value: String) {
    val context =
        this as? DotnetSolutionContext
            ?: error("marker { ... } can only be invoked within a .NET solution block.")
    context.put(TestSolutionExtension::class, TestSolutionExtension(listOf(value)))
}

class DotnetDslTests :
    StringSpec({
        "services-level dotnet blocks merge into defaults extension" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET8)
                }
            }

            builder.services {
                dotnet {
                    solutions {
                        "Platform" {}
                    }
                }
            }

            val extension = builder.model.get<ServicesExtension>()!!
            val defaults = extension.get<DotnetDefaultsExtension>()!!

            defaults.target shouldBe NET8
            defaults.solutions.keys shouldContainExactly listOf("Platform")
        }

        "services-level dotnet blocks merge matching solution declarations by name" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    solutions {
                        "Platform" {
                            marker("left")
                        }
                    }
                }
            }

            builder.services {
                dotnet {
                    solutions {
                        "Platform" {
                            marker("right")
                        }
                    }
                }
            }

            val extension = builder.model.get<ServicesExtension>()!!
            val solution = extension.get<DotnetDefaultsExtension>()!!.requireSolution("Platform")

            solution.get<TestSolutionExtension>()!!.values shouldContainExactly listOf("left", "right")
        }

        "service dotnet blocks merge into a single service extension" {
            val builder = MicrosmithBuilder()

            builder.services {
                "UserService" {
                    dotnet {
                        target(NET9)
                    }

                    dotnet {
                        solution("Platform")
                        project("UserService.Api")
                        models {
                            "User" {
                                string("id")
                                int("age")
                                "manager" ref "User"
                            }
                        }
                    }
                }
            }

            val extension = builder.model.get<ServicesExtension>()!!
            val service = extension.require("UserService")
            val dotnet = service.model.get<DotnetServiceExtension>()!!

            dotnet.target shouldBe NET9
            dotnet.solution shouldBe "Platform"
            dotnet.project shouldBe "UserService.Api"
            dotnet.models.keys shouldContainExactly listOf("User")
            val userModel = requireNotNull(dotnet.models["User"])
            userModel.fields.map(DotnetField::name) shouldContainExactly listOf("id", "age", "manager")
            userModel.fields.last().type shouldBe DotnetFieldType.Reference("User")
        }

        "duplicate service dotnet model names are rejected" {
            val builder = MicrosmithBuilder()

            shouldThrow<IllegalArgumentException> {
                builder.services {
                    "UserService" {
                        dotnet {
                            project("UserService.Api")
                            models {
                                "User" {
                                    string("id")
                                }
                            }
                        }

                        dotnet {
                            models {
                                "User" {
                                    string("name")
                                }
                            }
                        }
                    }
                }
            }
        }
    })
