package me.liam.microsmith.gen.schemas.protobuf

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import me.liam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import me.liam.microsmith.dsl.schemas.protobuf.field.Cardinality
import me.liam.microsmith.dsl.schemas.protobuf.field.MapField
import me.liam.microsmith.dsl.schemas.protobuf.field.MapType
import me.liam.microsmith.dsl.schemas.protobuf.field.OneofField
import me.liam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import me.liam.microsmith.dsl.schemas.protobuf.field.Reference
import me.liam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import me.liam.microsmith.dsl.schemas.protobuf.field.ScalarField
import me.liam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax
import me.liam.microsmith.dsl.schemas.protobuf.types.Enum
import me.liam.microsmith.dsl.schemas.protobuf.types.EnumValue
import me.liam.microsmith.dsl.schemas.protobuf.types.Message
import me.liam.microsmith.gen.schemas.protobuf.emission.ProtobufEmitter
import me.liam.microsmith.gen.schemas.protobuf.emission.ProtobufFieldNumbers
import me.liam.microsmith.gen.files.TemporaryDirectory
import kotlin.io.use

class ProtobufEmitterTests :
    StringSpec({
        val emitter = ProtobufEmitter()

        suspend fun emit(schema: ProtobufSchema): Pair<String, String> {
            TemporaryDirectory.create().use { space ->
                val generated = with(emitter) { schema.emit(space) }
                return generated.relativePath.toString().replace("\\", "/") to generated.contents.toString(Charsets.UTF_8)
            }
        }

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

            val (_, contents) = emit(schema)
            val imports = contents.lineSequence().filter { it.startsWith("import") }.toList()

            imports shouldContainExactly
                listOf(
                    """import "alpha/Beta.proto";""",
                    """import "zeta/Item.proto";"""
                )
        }

        "does not emit package for unqualified schema names and uses flat proto path" {
            val schema =
                ProtobufSchema(
                    "User",
                    Message(
                        "User",
                        fields = listOf(ScalarField("id", 1, PrimitiveType.INT32))
                    )
                )

            val (path, contents) = emit(schema)

            path shouldBe "proto/User.proto"
            contents.shouldNotContain("package ")
            contents.shouldContain("message User {")
        }

        "renders message fields oneofs and reserved blocks with explicit protobuf types" {
            val schema =
                ProtobufSchema(
                    "pkg.Contact",
                    Message(
                        name = "Contact",
                        fields =
                            listOf(
                                ScalarField("id", 2, PrimitiveType.INT64),
                                ReferenceField("status", 3, Reference("pkg.Status"), Cardinality.OPTIONAL),
                                MapField("labels", 4, MapType(PrimitiveType.STRING, PrimitiveType.STRING))
                            ),
                        oneofs =
                            listOf(
                                Oneof(
                                    "channel",
                                    listOf(
                                        OneofField("email", 5, PrimitiveType.STRING),
                                        OneofField("phone", 6, Reference("pkg.Phone"))
                                    )
                                )
                            ),
                        reserved =
                            listOf(
                                ReservedName("legacy_name"),
                                ReservedIndex(1),
                                ReservedRange(10..20),
                                ReservedToMax(100)
                            )
                    )
                )

            val (_, contents) = emit(schema)
            val imports = contents.lineSequence().filter { it.startsWith("import") }.toList()

            imports shouldContainExactly
                listOf(
                    """import "pkg/Phone.proto";""",
                    """import "pkg/Status.proto";"""
                )
            contents.shouldContain("reserved \"legacy_name\";")
            contents.shouldContain("reserved 1, 10 to 20, 100 to max;")
            contents.shouldContain("int64 id = 2;")
            contents.shouldContain("optional pkg.Status status = 3;")
            contents.shouldContain("map<string, string> labels = 4;")
            contents.shouldContain("oneof channel {")
            contents.shouldContain("pkg.Phone phone = 6;")
        }

        "deduplicates imports and skips self imports" {
            val schema =
                ProtobufSchema(
                    "pkg.User",
                    Message(
                        "User",
                        fields =
                            listOf(
                                ReferenceField("selfRef", 1, Reference("pkg.User")),
                                ReferenceField("address", 2, Reference("pkg.Address")),
                                MapField("addressMap", 3, MapType(PrimitiveType.STRING, Reference("pkg.Address")))
                            ),
                        oneofs =
                            listOf(
                                Oneof(
                                    "routing",
                                    listOf(
                                        OneofField("selfChoice", 4, Reference("pkg.User"))
                                    )
                                )
                            )
                    )
                )

            val (_, contents) = emit(schema)
            val imports = contents.lineSequence().filter { it.startsWith("import") }.toList()

            imports shouldContainExactly listOf("""import "pkg/Address.proto";""")
        }

        "renders enum schemas with package and reserved entries" {
            val schema =
                ProtobufSchema(
                    "pkg.Status",
                    Enum(
                        name = "Status",
                        values = listOf(EnumValue("UNSPECIFIED", 0), EnumValue("ACTIVE", 1)),
                        reserved = listOf(ReservedName("OLD_STATUS"), ReservedIndex(10))
                    )
                )

            val (path, contents) = emit(schema)

            path shouldBe "proto/pkg/Status.proto"
            contents.shouldContain("package pkg;")
            contents.shouldContain("enum Status {")
            contents.shouldContain("reserved \"OLD_STATUS\";")
            contents.shouldContain("reserved 10;")
            contents.shouldContain("UNSPECIFIED = 0;")
            contents.shouldContain("ACTIVE = 1;")
        }

        "fails fast when schema name is malformed" {
            val malformedSchema = ProtobufSchema(".broken", Message("broken"))

            shouldThrow<IllegalArgumentException> {
                emit(malformedSchema)
            }
        }

        "fails fast when reference name is malformed" {
            val schema =
                ProtobufSchema(
                    "pkg.User",
                    Message(
                        "User",
                        fields = listOf(ReferenceField("manager", 1, Reference("bad ref")))
                    )
                )

            shouldThrow<IllegalArgumentException> {
                emit(schema)
            }
        }

        "fails fast when oneof field is placed in top-level fields" {
            val schema =
                ProtobufSchema(
                    "pkg.Invalid",
                    Message(
                        "Invalid",
                        fields = listOf(OneofField("choice", 1, PrimitiveType.STRING))
                    )
                )

            shouldThrow<IllegalStateException> {
                emit(schema)
            }
        }

        "rejects forbidden protobuf field range for message fields" {
            val schema =
                ProtobufSchema(
                    "pkg.ForbiddenField",
                    Message(
                        "ForbiddenField",
                        fields = listOf(ScalarField("id", 19_000, PrimitiveType.INT32))
                    )
                )

            shouldThrow<IllegalArgumentException> {
                emit(schema)
            }
        }

        "rejects field numbers above protobuf maximum" {
            val schema =
                ProtobufSchema(
                    "pkg.TooLargeField",
                    Message(
                        "TooLargeField",
                        fields =
                            listOf(
                                ScalarField(
                                    "id",
                                    ProtobufFieldNumbers.MAX_FIELD_NUMBER + 1,
                                    PrimitiveType.INT32
                                )
                            )
                    )
                )

            shouldThrow<IllegalArgumentException> {
                emit(schema)
            }
        }

        "requires enum first declared value to use index 0" {
            val schema =
                ProtobufSchema(
                    "pkg.BadEnum",
                    Enum(
                        name = "BadEnum",
                        values = listOf(EnumValue("ACTIVE", 1), EnumValue("UNSPECIFIED", 0))
                    )
                )

            shouldThrow<IllegalArgumentException> {
                emit(schema)
            }
        }

        "resolves unqualified reference imports into current package" {
            val schema =
                ProtobufSchema(
                    "pkg.Order",
                    Message(
                        "Order",
                        fields = listOf(ReferenceField("customer", 1, Reference("Customer")))
                    )
                )

            val (_, contents) = emit(schema)
            val imports = contents.lineSequence().filter { it.startsWith("import") }.toList()

            imports shouldContainExactly listOf("""import "pkg/Customer.proto";""")
            contents.shouldContain("Customer customer = 1;")
        }

        "rejects invalid protobuf identifiers" {
            val schema =
                ProtobufSchema(
                    "pkg.InvalidIdentifier",
                    Message(
                        "InvalidIdentifier",
                        fields = listOf(ScalarField("123bad", 1, PrimitiveType.INT32))
                    )
                )

            shouldThrow<IllegalArgumentException> {
                emit(schema)
            }
        }
    })
