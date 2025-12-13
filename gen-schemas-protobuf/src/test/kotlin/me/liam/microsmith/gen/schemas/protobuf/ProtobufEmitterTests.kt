package me.liam.microsmith.gen.schemas.protobuf

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.runBlocking
import me.liam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import me.liam.microsmith.dsl.schemas.protobuf.field.MapField
import me.liam.microsmith.dsl.schemas.protobuf.field.MapType
import me.liam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import me.liam.microsmith.dsl.schemas.protobuf.field.Reference
import me.liam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import me.liam.microsmith.dsl.schemas.protobuf.types.Message
import me.liam.microsmith.gen.files.TemporaryDirectory
import kotlin.io.use

class ProtobufEmitterTests :
    StringSpec({
        "emits imports in deterministic order" {
            val schema =
                ProtobufSchema(
                    "pkg.Bar",
                    Message(
                        "Bar",
                        fields =
                            listOf(
                                ReferenceField("refB", 1, Reference("zeta.Item")),
                                MapField("mapRef", 2, MapType(PrimitiveType.STRING, Reference("alpha.Beta")))
                            )
                    )
                )

            val emitter = ProtobufEmitter()
            runBlocking {
                TemporaryDirectory.create().use { space ->
                    val generated = with(emitter) { schema.emit(space) }

                    val imports =
                        generated
                            .contents
                            .toString(Charsets.UTF_8)
                            .lineSequence()
                            .filter { it.startsWith("import") }
                            .toList()

                    imports shouldContainExactly
                        listOf(
                            """import "alpha/Beta.proto";""",
                            """import "zeta/Item.proto";"""
                        )
                }
            }
        }
    })
