package io.github.lmliam.microsmith.runtime.scripting.symbols.discovery
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.EnumRef
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.MessageRef
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ScriptSymbolDiscoveryTests :
    StringSpec({
        "discovers qualified protobuf symbols from namespace and version scopes" {
            val symbols = ScriptSymbolDiscovery.discover(
                sourceName = "schema.microsmith.kts",
                sourceText = """
                microsmith {
                    schemas {
                        protobuf {
                            "acme.user" {
                                version(2) {
                                    message("User")
                                    enum("Role")
                                }
                            }
                        }
                    }
                }
                """.trimIndent(),
                registry = ScriptSymbolContributorRegistry.of(
                    listOf(
                        ProtobufScriptSymbolContributor,
                    ),
                ),
            )

            symbols.map { it.definition.propertyName } shouldContainExactly listOf("Role", "User")

            symbols.single { it.definition.propertyName == "User" }.definition.apply {
                valueType shouldBe MessageRef::class
                valueKey shouldBe "acme.user.v2.User"
            }

            symbols.single { it.definition.propertyName == "Role" }.definition.apply {
                valueType shouldBe EnumRef::class
                valueKey shouldBe "acme.user.v2.Role"
            }
        }

        "ignores dynamic declaration names because they cannot become lexical symbols" {
            val symbols = ScriptSymbolDiscovery.discover(
                sourceName = "schema.microsmith.kts",
                sourceText = """
                    val name = "User"
                    microsmith {
                        schemas {
                            protobuf {
                                message(name)
                            }
                        }
                    }
                """.trimIndent(),
                registry = ScriptSymbolContributorRegistry.of(
                    listOf(
                        ProtobufScriptSymbolContributor,
                    ),
                ),
            )

            symbols shouldBe emptyList()
        }

        "rejects ambiguous automatic symbols across protobuf namespaces" {
            val failure = shouldThrow<IllegalArgumentException> {
                ScriptSymbolDiscovery.discover(
                    sourceName = "schema.microsmith.kts",
                    sourceText = """
                        microsmith {
                            schemas {
                                protobuf {
                                    "one" {
                                        message("User")
                                    }
                                    "two" {
                                        message("User")
                                    }
                                }
                            }
                        }
                    """.trimIndent(),
                    registry = ScriptSymbolContributorRegistry.of(
                        listOf(
                            ProtobufScriptSymbolContributor,
                        ),
                    ),
                )
            }

            failure.message shouldBe "Automatic script symbol 'User' is ambiguous between: " +
                "microsmith.protobuf: one.User, " +
                "microsmith.protobuf: two.User"
        }

        "parses hexadecimal protobuf version literals" {
            val symbols =
                ScriptSymbolDiscovery.discover(
                    sourceName = "schema.microsmith.kts",
                    sourceText =
                    """
                microsmith {
                    schemas {
                        protobuf {
                            "acme" {
                                version(0x10) {
                                    message("User")
                                }
                            }
                        }
                    }
                }
                    """.trimIndent(),
                    registry =
                    ScriptSymbolContributorRegistry.of(
                        listOf(ProtobufScriptSymbolContributor),
                    ),
                )

            symbols
                .single { it.definition.propertyName == "User" }
                .definition
                .valueKey shouldBe "acme.v16.User"
        }
    })
