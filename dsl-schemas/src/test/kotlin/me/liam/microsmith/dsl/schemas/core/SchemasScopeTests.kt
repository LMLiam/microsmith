package me.liam.microsmith.dsl.schemas.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import me.liam.microsmith.dsl.core.MicrosmithBuilder

private enum class ScopeTestSchemaTypes(
    override val typeName: String
) : SchemaType {
    PROTOBUF("protobuf"),
    JSON("json")
}

private data class ScopeFakeSchema(
    override val type: SchemaType,
    override val name: String
) : Schema

private fun SchemasScope.fake(
    type: SchemaType,
    name: String
) {
    val builder = this as SchemasBuilder
    builder.register(ScopeFakeSchema(type, name))
}

class SchemasScopeTests :
    StringSpec({
        "schemas block attaches SchemasExtension to builder" {
            val builder = MicrosmithBuilder()

            builder.schemas {
                fake(ScopeTestSchemaTypes.PROTOBUF, "User")
            }

            val ext = builder.model.get<SchemasExtension>()
            ext shouldBe SchemasExtension(setOf(ScopeFakeSchema(ScopeTestSchemaTypes.PROTOBUF, "User")))
        }

        "schemas block can register multiple schemas" {
            val builder = MicrosmithBuilder()

            builder.schemas {
                fake(ScopeTestSchemaTypes.PROTOBUF, "User")
                fake(ScopeTestSchemaTypes.JSON, "User")
            }

            val ext = builder.model.get<SchemasExtension>()
            ext shouldBe
                SchemasExtension(
                    setOf(
                        ScopeFakeSchema(ScopeTestSchemaTypes.PROTOBUF, "User"),
                        ScopeFakeSchema(ScopeTestSchemaTypes.JSON, "User")
                    )
                )
        }

        "multiple schemas blocks are merged" {
            val builder = MicrosmithBuilder()

            builder.schemas {
                fake(ScopeTestSchemaTypes.PROTOBUF, "User")
            }

            builder.schemas {
                fake(ScopeTestSchemaTypes.JSON, "User")
            }

            val ext = builder.model.get<SchemasExtension>()
            ext shouldBe
                SchemasExtension(
                    setOf(
                        ScopeFakeSchema(ScopeTestSchemaTypes.PROTOBUF, "User"),
                        ScopeFakeSchema(ScopeTestSchemaTypes.JSON, "User")
                    )
                )
        }
    })