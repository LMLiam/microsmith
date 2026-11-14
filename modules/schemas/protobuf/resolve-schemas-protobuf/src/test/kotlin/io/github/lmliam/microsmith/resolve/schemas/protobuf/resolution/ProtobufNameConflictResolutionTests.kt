package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import io.github.lmliam.microsmith.dsl.schemas.SchemasExtension
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.OneofField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.EnumValue
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.types.shouldBeTypeOf

class ProtobufNameConflictResolutionTests :
    StringSpec(
        {
            val resolver = ProtobufSchemasResolver()

            fun resolve(vararg schemas: ProtobufSchema) = resolver.resolve(SchemasExtension(schemas.toSet()))

            fun DomainResolution<*>.issues() = shouldBeTypeOf<DomainResolution.Failure>().issues.toList()

            fun DomainResolution<*>.shouldSucceed() =
                shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()

            fun message(vararg fieldNames: String) =
                ProtobufSchema(
                    "M",
                    Message(
                        "M",
                        fieldNames.mapIndexed { index, name -> ScalarField(name, index + 1, PrimitiveType.STRING) },
                    ),
                )

            fun messageWithOneof(plain: List<String>, oneofFields: List<String>) =
                ProtobufSchema(
                    "M",
                    Message(
                        "M",
                        plain.mapIndexed { index, name -> ScalarField(name, index + 1, PrimitiveType.STRING) },
                        listOf(
                            Oneof(
                                "choice",
                                oneofFields.mapIndexed { index, name ->
                                    OneofField(name, plain.size + index + 1, PrimitiveType.STRING)
                                },
                            ),
                        ),
                    ),
                )

            fun enum(enumName: String, vararg valueNames: String) =
                ProtobufSchema(
                    enumName,
                    Enum(
                        enumName.substringAfterLast('.'),
                        valueNames.mapIndexed { index, name -> EnumValue(name, index) },
                    ),
                )

            fun jsonCollision(jsonName: String, vararg fieldNames: String) =
                ProtobufResolutionIssue.FieldJsonNameCollision("M", jsonName, fieldNames.toList())

            fun enumCollision(enumName: String, vararg valueNames: String) =
                ProtobufResolutionIssue.EnumValueNameCollision(enumName, valueNames.toList())

            "rejects fields whose names differ only by an underscore before a letter" {
                resolve(message("foo_bar", "fooBar")).issues() shouldContainExactly
                    listOf(jsonCollision("fooBar", "foo_bar", "fooBar"))
            }

            "rejects fields that capitalise the letter after an underscore" {
                resolve(message("foo_bar", "foo_Bar")).issues() shouldContainExactly
                    listOf(jsonCollision("fooBar", "foo_bar", "foo_Bar"))
            }

            "rejects fields that repeat an underscore" {
                resolve(message("foo__bar", "foo_bar")).issues() shouldContainExactly
                    listOf(jsonCollision("fooBar", "foo__bar", "foo_bar"))
            }

            "rejects a field with a leading underscore and a capitalised field" {
                resolve(message("_foo", "Foo")).issues() shouldContainExactly
                    listOf(jsonCollision("Foo", "_foo", "Foo"))
            }

            "rejects a field with a trailing underscore and its stem" {
                resolve(message("foo_", "foo")).issues() shouldContainExactly
                    listOf(jsonCollision("foo", "foo_", "foo"))
            }

            "rejects fields whose underscore precedes a digit" {
                resolve(message("foo_1a", "foo1a")).issues() shouldContainExactly
                    listOf(jsonCollision("foo1a", "foo_1a", "foo1a"))
            }

            "accepts fields that differ by an underscore alone" {
                resolve(message("foo_bar", "foobar")).shouldSucceed()
            }

            "accepts fields that differ only by case" { resolve(message("Foo", "foo")).shouldSucceed() }

            "rejects a oneof field that shares a JSON name with a plain field" {
                resolve(messageWithOneof(listOf("foo_bar"), listOf("fooBar"))).issues() shouldContainExactly
                    listOf(jsonCollision("fooBar", "foo_bar", "fooBar"))
            }

            "rejects two fields of one oneof that share a JSON name" {
                resolve(messageWithOneof(emptyList(), listOf("foo_bar", "fooBar"))).issues() shouldContainExactly
                    listOf(jsonCollision("fooBar", "foo_bar", "fooBar"))
            }

            "rejects a map field that shares a JSON name with a plain field" {
                val schema =
                    ProtobufSchema(
                        "M",
                        Message(
                            "M",
                            listOf(
                                ScalarField("foo_bar", 1, PrimitiveType.STRING),
                                MapField("fooBar", 2, MapType(PrimitiveType.STRING, PrimitiveType.STRING)),
                            ),
                        ),
                    )

                resolve(schema).issues() shouldContainExactly listOf(jsonCollision("fooBar", "foo_bar", "fooBar"))
            }

            "reports every field that shares one JSON name in a single issue" {
                resolve(message("foo_bar", "fooBar", "foo__bar")).issues() shouldContainExactly
                    listOf(jsonCollision("fooBar", "foo_bar", "fooBar", "foo__bar"))
            }

            "reports each shared JSON name separately in input order" {
                resolve(message("a_b", "x_y", "aB", "xY")).issues() shouldContainExactly
                    listOf(jsonCollision("aB", "a_b", "aB"), jsonCollision("xY", "x_y", "xY"))
            }

            "lists a repeated field name once in a JSON name collision" {
                resolve(message("foo_bar", "fooBar", "foo_bar")).issues() shouldContainExactlyInAnyOrder
                    listOf(
                        ProtobufResolutionIssue.DuplicateFieldNames("M", listOf("foo_bar")),
                        jsonCollision("fooBar", "foo_bar", "fooBar"),
                    )
            }

            "accepts fields of different messages that share a JSON name" {
                val first = ProtobufSchema("X", Message("X", listOf(ScalarField("foo_bar", 1, PrimitiveType.STRING))))
                val second = ProtobufSchema("Y", Message("Y", listOf(ScalarField("fooBar", 1, PrimitiveType.STRING))))

                resolve(first, second).shouldSucceed()
            }

            "does not report a JSON name collision for fields with invalid names" {
                resolve(message("é_x", "éX")).issues() shouldContainExactlyInAnyOrder
                    listOf(
                        ProtobufResolutionIssue.InvalidIdentifier(
                            "M",
                            ProtobufResolutionIssue.IdentifierLocation.Field("é_x"),
                            "é_x",
                        ),
                        ProtobufResolutionIssue.InvalidIdentifier(
                            "M",
                            ProtobufResolutionIssue.IdentifierLocation.Field("éX"),
                            "éX",
                        ),
                    )
            }

            "rejects enum values that differ only by the enum name prefix" {
                resolve(enum("Color", "COLOR_UNSPECIFIED", "COLOR_RED", "RED")).issues() shouldContainExactly
                    listOf(enumCollision("Color", "COLOR_RED", "RED"))
            }

            "rejects enum values that differ only by case" {
                resolve(enum("Color", "COLOR_UNSPECIFIED", "RED", "red")).issues() shouldContainExactly
                    listOf(enumCollision("Color", "RED", "red"))
            }

            "rejects an enum value with a prefix and a value in another case" {
                resolve(enum("Color", "COLOR_UNSPECIFIED", "COLOR_RED", "Red")).issues() shouldContainExactly
                    listOf(enumCollision("Color", "COLOR_RED", "Red"))
            }

            "accepts enum values whose words are split differently" {
                resolve(enum("E", "E_UNSPECIFIED", "FOO_BAR", "FooBar")).shouldSucceed()
            }

            "accepts an enum value named like its enum next to another value" {
                resolve(enum("Color", "COLOR", "RED")).shouldSucceed()
            }

            "accepts an enum value named like its enum next to the unspecified value" {
                resolve(enum("Color", "COLOR_UNSPECIFIED", "COLOR")).shouldSucceed()
            }

            "rejects an enum value that is the enum prefix and a trailing underscore next to the bare enum name" {
                resolve(enum("Color", "COLOR_", "COLOR")).issues() shouldContainExactly
                    listOf(enumCollision("Color", "COLOR_", "COLOR"))
            }

            "rejects an enum value that repeats the enum name next to the bare enum name" {
                resolve(enum("Color", "COLOR_UNSPECIFIED", "COLOR_COLOR", "COLOR")).issues() shouldContainExactly
                    listOf(enumCollision("Color", "COLOR_COLOR", "COLOR"))
            }

            "strips the enum name prefix only once" {
                resolve(enum("Color", "COLOR_UNSPECIFIED", "COLOR_COLOR_RED", "COLOR_RED")).shouldSucceed()
            }

            "rejects enum values that repeat an underscore" {
                resolve(enum("Color", "COLOR_UNSPECIFIED", "A__B", "A_B")).issues() shouldContainExactly
                    listOf(enumCollision("Color", "A__B", "A_B"))
            }

            "accepts enum values that differ by an underscore alone" {
                resolve(enum("Color", "COLOR_UNSPECIFIED", "A_B", "AB")).shouldSucceed()
            }

            "rejects enum values whose underscore precedes a digit" {
                resolve(enum("Color", "COLOR_UNSPECIFIED", "A_1B", "A1B")).issues() shouldContainExactly
                    listOf(enumCollision("Color", "A_1B", "A1B"))
            }

            "matches the enum name prefix across underscores in the value" {
                resolve(enum("Color", "COLOR_UNSPECIFIED", "CO_LOR_RED", "RED")).issues() shouldContainExactly
                    listOf(enumCollision("Color", "CO_LOR_RED", "RED"))
            }

            "matches the enum name prefix ignoring underscores in the enum name" {
                resolve(enum("Foo_Bar", "FOO_BAR_UNSPECIFIED", "FOOBAR_X", "X")).issues() shouldContainExactly
                    listOf(enumCollision("Foo_Bar", "FOOBAR_X", "X"))
            }

            "accepts values with the same remainder in different enums" {
                resolve(enum("A", "A_X"), enum("B", "X")).shouldSucceed()
            }

            "reports every enum value that shares one key in a single issue" {
                resolve(enum("Color", "COLOR_UNSPECIFIED", "COLOR_RED", "RED", "Red")).issues() shouldContainExactly
                    listOf(enumCollision("Color", "COLOR_RED", "RED", "Red"))
            }

            "does not report an enum value collision for values with invalid names" {
                resolve(enum("Color", "COLOR_UNSPECIFIED", "é", "É")).issues() shouldContainExactlyInAnyOrder
                    listOf(
                        ProtobufResolutionIssue.InvalidIdentifier(
                            "Color",
                            ProtobufResolutionIssue.IdentifierLocation.EnumValue("é"),
                            "é",
                        ),
                        ProtobufResolutionIssue.InvalidIdentifier(
                            "Color",
                            ProtobufResolutionIssue.IdentifierLocation.EnumValue("É"),
                            "É",
                        ),
                    )
            }

            "reports name conflicts after the issues of each schema and before symbol collisions" {
                val broken =
                    ProtobufSchema(
                        "p.Broken",
                        Message(
                            "Broken",
                            listOf(
                                ScalarField("a", 1, PrimitiveType.STRING),
                                ScalarField("a", 2, PrimitiveType.STRING),
                            ),
                        ),
                    )
                val jsonClash =
                    ProtobufSchema(
                        "p.Clash",
                        Message(
                            "Clash",
                            listOf(
                                ScalarField("foo_bar", 1, PrimitiveType.STRING),
                                ScalarField("fooBar", 2, PrimitiveType.STRING),
                            ),
                        ),
                    )

                resolve(
                        enum("p.E2", "E2_UNSPECIFIED", "D"),
                        jsonClash,
                        enum("p.E1", "E1_UNSPECIFIED", "E1_X", "x", "D"),
                        broken,
                    )
                    .issues() shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.DuplicateFieldNames("p.Broken", listOf("a")),
                        ProtobufResolutionIssue.FieldJsonNameCollision(
                            "p.Clash",
                            "fooBar",
                            listOf("foo_bar", "fooBar"),
                        ),
                        ProtobufResolutionIssue.EnumValueNameCollision("p.E1", listOf("E1_X", "x")),
                        ProtobufResolutionIssue.SymbolNameCollision(
                            "p",
                            "D",
                            listOf(
                                ProtobufResolutionIssue.SymbolDeclaration.EnumValue("p.E1", "D"),
                                ProtobufResolutionIssue.SymbolDeclaration.EnumValue("p.E2", "D"),
                            ),
                        ),
                    )
            }
        },
    )
