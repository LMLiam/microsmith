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
            "discovers a unique declaration under both its plain and qualified symbol" {
                val result = discover("/scripting/symbols/discovery/qualified-protobuf-symbols.microsmith.kts")

                result.symbols.map { it.propertyName } shouldContainExactly
                    listOf("Role", "User", "acme_user_v2_Role", "acme_user_v2_User")

                result.symbols.map { it.definition.valueKey }.distinct() shouldContainExactly
                    listOf("acme.user.v2.Role", "acme.user.v2.User")

                result.symbols.single { it.propertyName == "User" }.definition.valueType shouldBe MessageRef::class
                result.symbols.single { it.propertyName == "Role" }.definition.valueType shouldBe EnumRef::class
            }

            "discovers one symbol for a declaration outside any namespace" {
                val result = discoverText("microsmith { schemas { protobuf { message(\"User\") } } }")

                result.symbols.map { it.propertyName } shouldContainExactly listOf("User")
            }

            "discovers same-named messages in different namespaces under their qualified symbols" {
                val result = discover("/scripting/symbols/discovery/same-name-namespaces.microsmith.kts")

                result.symbols.associate { it.propertyName to it.definition.valueKey } shouldBe
                    mapOf("one_User" to "one.User", "two_User" to "two.User")
            }

            "discovers same-named messages in different versions under their qualified symbols" {
                val result = discover("/scripting/symbols/discovery/same-name-versions.microsmith.kts")

                result.symbols.associate { it.propertyName to it.definition.valueKey } shouldBe
                    mapOf("acme_v1_User" to "acme.v1.User", "acme_v2_User" to "acme.v2.User")
            }

            "reports omitted plain names with their qualified alternatives" {
                val result = discover("/scripting/symbols/discovery/same-name-namespaces.microsmith.kts")

                result.omittedPlainNames shouldBe mapOf("User" to listOf("one_User", "two_User"))
            }

            "prefers a qualified symbol over another declaration's plain name" {
                val result = discover("/scripting/symbols/discovery/plain-equals-other-qualified.microsmith.kts")

                result.symbols.associate { it.propertyName to it.definition.valueKey } shouldBe
                    mapOf(
                        "Y" to "x.Y",
                        "x_Y" to "x.Y",
                        "z_x_Y" to "z.x_Y",
                    )
            }

            "rejects a message and an enum that share a qualified name" {
                shouldThrow<IllegalArgumentException> {
                    discover("/scripting/symbols/discovery/kind-collision.microsmith.kts")
                }
            }

            "rejects different declarations that join to the same qualified name" {
                shouldThrow<IllegalArgumentException> {
                    discover("/scripting/symbols/discovery/underscore-collision.microsmith.kts")
                }
            }

            "ignores dynamic declaration names because they cannot become lexical symbols" {
                discover("/scripting/symbols/discovery/dynamic-declaration-name.microsmith.kts").symbols shouldBe
                    emptyList()
            }

            "parses hexadecimal protobuf version literals" {
                val result = discover("/scripting/symbols/discovery/hexadecimal-version.microsmith.kts")

                result.symbols.single { it.propertyName == "User" }.definition.valueKey shouldBe "acme.v16.User"
            }
        },
    )

private fun discover(resource: String): ScriptSymbolDiscoveryResult = discoverText(scriptResource(resource))

private fun discoverText(sourceText: String): ScriptSymbolDiscoveryResult =
    ScriptSymbolDiscovery.discover(
        sourceName = "schema.microsmith.kts",
        sourceText = sourceText,
        registry = ScriptSymbolContributorRegistry.of(listOf(ProtobufScriptSymbolContributor)),
    )

private fun scriptResource(resource: String): String =
    checkNotNull(ScriptSymbolDiscoveryTests::class.java.getResource(resource)) { "Missing test resource $resource" }
        .readText()
