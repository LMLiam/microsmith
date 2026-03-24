package io.github.lmliam.microsmith.dsl.services.dotnet.core

import io.github.lmliam.microsmith.dsl.core.MergeableExtension
import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.core.services
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

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

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

            val extension = builder.requireServicesExtension()
            val defaults = requireNotNull(extension.get<DotnetDefaultsExtension>())

            defaults.target shouldBe NET8
            defaults.solutions.keys shouldContainExactly listOf("Platform")
        }

        "services-level dotnet solutions support both string-invoke and solution helpers" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    solutions {
                        "Platform" {}
                        solution("Operations") {}
                    }
                }
            }

            val defaults = requireNotNull(builder.requireServicesExtension().get<DotnetDefaultsExtension>())

            defaults.solutions.keys shouldContainExactly listOf("Platform", "Operations")
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

            val extension = builder.requireServicesExtension()
            val solution = requireNotNull(extension.get<DotnetDefaultsExtension>()).requireSolution("Platform")

            requireNotNull(solution.get<TestSolutionExtension>()).values shouldContainExactly listOf("left", "right")
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
                                "owner" references "User"
                            }
                        }
                    }
                }
            }

            val extension = builder.requireServicesExtension()
            val service = extension.require("UserService")
            val dotnet = requireNotNull(service.model.get<DotnetServiceExtension>())

            dotnet.target shouldBe NET9
            dotnet.solution shouldBe "Platform"
            dotnet.project shouldBe "UserService.Api"
            dotnet.models.keys shouldContainExactly listOf("User")
            val userModel = requireNotNull(dotnet.models["User"])
            userModel.fields.map(DotnetField::name) shouldContainExactly listOf("id", "age", "manager", "owner")
            userModel.fields.drop(2).map(DotnetField::type) shouldContainExactly listOf(
                DotnetFieldType.Reference("User"),
                DotnetFieldType.Reference("User"),
            )
        }

        "dotnet models support both string-invoke and model helpers" {
            val builder = MicrosmithBuilder()

            builder.services {
                "UserService" {
                    dotnet {
                        project("UserService.Api")
                        models {
                            "User" {
                                string("id")
                            }

                            model("Address") {
                                string("line1")
                            }
                        }
                    }
                }
            }

            val models =
                builder
                    .requireServicesExtension()
                    .require("UserService")
                    .model
                    .let { requireNotNull(it.get<DotnetServiceExtension>()) }
                    .models

            models.keys shouldContainExactly listOf("User", "Address")
            requireNotNull(models["User"]).fields.map(DotnetField::name) shouldContainExactly listOf("id")
            requireNotNull(models["Address"]).fields.map(DotnetField::name) shouldContainExactly listOf("line1")
        }

        "dotnet models support the csharp scalar set" {
            val builder = MicrosmithBuilder()

            builder.services {
                "UserService" {
                    dotnet {
                        project("UserService.Api")
                        models {
                            "Example" {
                                string("name")
                                char("initial")
                                byte("level")
                                sbyte("delta")
                                short("rank")
                                ushort("maxRank")
                                int("count")
                                uint("capacity")
                                long("total")
                                ulong("lifetimeTotal")
                                nint("offset")
                                nuint("pageSize")
                                float("ratio")
                                double("average")
                                decimal("amount")
                                bool("active")
                                guid("id")
                                dateOnly("startDate")
                                timeOnly("startTime")
                                dateTime("createdAt")
                                dateTimeOffset("publishedAt")
                                timeSpan("duration")
                            }
                        }
                    }
                }
            }

            val exampleModel =
                builder
                    .requireServicesExtension()
                    .require("UserService")
                    .model
                    .let { requireNotNull(it.get<DotnetServiceExtension>()) }
                    .requireModel("Example")

            exampleModel.fields.map(DotnetField::type) shouldContainExactly listOf(
                DotnetFieldType.String,
                DotnetFieldType.Char,
                DotnetFieldType.Byte,
                DotnetFieldType.SignedByte,
                DotnetFieldType.Short,
                DotnetFieldType.UnsignedShort,
                DotnetFieldType.Int,
                DotnetFieldType.UnsignedInt,
                DotnetFieldType.Long,
                DotnetFieldType.UnsignedLong,
                DotnetFieldType.NativeInt,
                DotnetFieldType.UnsignedNativeInt,
                DotnetFieldType.Float,
                DotnetFieldType.Double,
                DotnetFieldType.Decimal,
                DotnetFieldType.Bool,
                DotnetFieldType.Guid,
                DotnetFieldType.DateOnly,
                DotnetFieldType.TimeOnly,
                DotnetFieldType.DateTime,
                DotnetFieldType.DateTimeOffset,
                DotnetFieldType.TimeSpan,
            )
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

        "dotnet target validates supported tfms" {
            DotnetTarget.of("net10.0") shouldBe DotnetTarget.NET10
            DotnetTarget.of("net5.0") shouldBe DotnetTarget.NET5

            shouldThrow<IllegalArgumentException> {
                DotnetTarget.of("netstandard2.1")
            }

            shouldThrow<IllegalArgumentException> {
                DotnetTarget.of("netcoreapp3.1")
            }
        }
    })
