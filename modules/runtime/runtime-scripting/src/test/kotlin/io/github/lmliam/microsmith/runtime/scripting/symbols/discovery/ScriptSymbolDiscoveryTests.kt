package io.github.lmliam.microsmith.runtime.scripting.symbols.discovery

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.EnumRef
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.MessageRef
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ScriptSymbolDiscoveryTests :
    StringSpec(
        {
            "discovers qualified protobuf symbols from namespace and version scopes" {
                val symbols =
                    ScriptSymbolDiscovery.discover(
                        sourceName = "schema.microsmith.kts",
                        sourceText =
                            scriptResource("/scripting/symbols/discovery/qualified-protobuf-symbols.microsmith.kts"),
                        registry = ScriptSymbolContributorRegistry.of(listOf(ProtobufScriptSymbolContributor)),
                    )

                symbols.map { it.definition.propertyName } shouldContainExactly listOf("Role", "User")

                symbols
                    .single { it.definition.propertyName == "User" }
                    .definition
                    .apply {
                        valueType shouldBe MessageRef::class
                        valueKey shouldBe "acme.user.v2.User"
                    }

                symbols
                    .single { it.definition.propertyName == "Role" }
                    .definition
                    .apply {
                        valueType shouldBe EnumRef::class
                        valueKey shouldBe "acme.user.v2.Role"
                    }
            }

            "ignores dynamic declaration names because they cannot become lexical symbols" {
                val symbols =
                    ScriptSymbolDiscovery.discover(
                        sourceName = "schema.microsmith.kts",
                        sourceText =
                            scriptResource("/scripting/symbols/discovery/dynamic-declaration-name.microsmith.kts"),
                        registry = ScriptSymbolContributorRegistry.of(listOf(ProtobufScriptSymbolContributor)),
                    )

                symbols shouldBe emptyList()
            }

            "rejects ambiguous automatic symbols across protobuf namespaces" {
                val failure =
                    shouldThrow<IllegalArgumentException> {
                        ScriptSymbolDiscovery.discover(
                            sourceName = "schema.microsmith.kts",
                            sourceText =
                                scriptResource("/scripting/symbols/discovery/ambiguous-symbols.microsmith.kts"),
                            registry = ScriptSymbolContributorRegistry.of(listOf(ProtobufScriptSymbolContributor)),
                        )
                    }

                failure.message shouldBe
                    "Automatic script symbol 'User' is ambiguous between: " +
                        "microsmith.protobuf: one.User, " +
                        "microsmith.protobuf: two.User"
            }

            "parses hexadecimal protobuf version literals" {
                val symbols =
                    ScriptSymbolDiscovery.discover(
                        sourceName = "schema.microsmith.kts",
                        sourceText = scriptResource("/scripting/symbols/discovery/hexadecimal-version.microsmith.kts"),
                        registry = ScriptSymbolContributorRegistry.of(listOf(ProtobufScriptSymbolContributor)),
                    )

                symbols.single { it.definition.propertyName == "User" }.definition.valueKey shouldBe "acme.v16.User"
            }
        },
    )

private fun scriptResource(resource: String): String =
    checkNotNull(ScriptSymbolDiscoveryTests::class.java.getResource(resource)) { "Missing test resource $resource" }
        .readText()
