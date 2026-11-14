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
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.ServiceDeclaration
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue.SymbolDeclaration
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.types.shouldBeTypeOf

class ProtobufSymbolCollisionResolutionTests :
    StringSpec(
        {
            val resolver = ProtobufSchemasResolver()

            fun resolve(vararg schemas: ProtobufSchema) = resolver.resolve(SchemasExtension(schemas.toSet()))

            fun DomainResolution<*>.issues() = shouldBeTypeOf<DomainResolution.Failure>().issues.toList()

            fun string(name: String, number: Int) = ScalarField(name, number, PrimitiveType.STRING)

            fun mapField(name: String, number: Int) =
                MapField(name, number, MapType(PrimitiveType.STRING, PrimitiveType.STRING))

            fun oneof(name: String, vararg fields: Pair<String, Int>) =
                Oneof(name, fields.map { OneofField(it.first, it.second, PrimitiveType.STRING) })

            fun enum(schemaName: String, vararg values: String) =
                ProtobufSchema(
                    schemaName,
                    Enum(
                        schemaName.substringAfterLast('.'),
                        values.mapIndexed { index, name -> EnumValue(name, index) },
                    ),
                )

            fun message(
                schemaName: String,
                fields: List<ScalarField> = emptyList(),
                oneofs: List<Oneof> = emptyList(),
            ) = ProtobufSchema(schemaName, Message(schemaName.substringAfterLast('.'), fields, oneofs))

            "rejects a oneof named like a field" {
                val schema = message("M", listOf(string("x", 1)), listOf(oneof("x", "y" to 2)))

                resolve(schema).issues() shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.SymbolNameCollision(
                            "M",
                            "x",
                            listOf(SymbolDeclaration.Field("M", "x"), SymbolDeclaration.Oneof("M", "x")),
                        ),
                    )
            }

            "rejects a field named like the entry message of a map field" {
                val schema = ProtobufSchema("M", Message("M", listOf(mapField("foo", 1), string("FooEntry", 2))))

                resolve(schema).issues() shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.SymbolNameCollision(
                            "M",
                            "FooEntry",
                            listOf(SymbolDeclaration.Field("M", "FooEntry"), SymbolDeclaration.MapEntry("M", "foo")),
                        ),
                    )
            }

            "rejects a oneof named like the entry message of a map field" {
                val schema =
                    ProtobufSchema(
                        "M",
                        Message("M", listOf(mapField("foo", 1)), listOf(oneof("FooEntry", "y" to 2))),
                    )

                resolve(schema).issues() shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.SymbolNameCollision(
                            "M",
                            "FooEntry",
                            listOf(SymbolDeclaration.Oneof("M", "FooEntry"), SymbolDeclaration.MapEntry("M", "foo")),
                        ),
                    )
            }

            "rejects two map fields that synthesise the same entry message name" {
                val schema = ProtobufSchema("M", Message("M", listOf(mapField("foo_bar", 1), mapField("Foo_bar", 2))))

                resolve(schema).issues() shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.SymbolNameCollision(
                            "M",
                            "FooBarEntry",
                            listOf(
                                SymbolDeclaration.MapEntry("M", "foo_bar"),
                                SymbolDeclaration.MapEntry("M", "Foo_bar"),
                            ),
                        ),
                    )
            }

            "accepts a field whose name differs from the map entry name only by case" {
                val schema = ProtobufSchema("M", Message("M", listOf(mapField("foo", 1), string("fooEntry", 2))))

                resolve(schema).shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()
            }

            "accepts a field named like its own message" {
                resolve(message("M", listOf(string("M", 1))))
                    .shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()
            }

            "accepts the same field name in different messages of one package" {
                resolve(message("p.X", listOf(string("a", 1))), message("p.Y", listOf(string("a", 1))))
                    .shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()
            }

            "rejects the same value name in two enums of one package" {
                resolve(enum("p.E1", "A"), enum("p.E2", "A")).issues() shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.SymbolNameCollision(
                            "p",
                            "A",
                            listOf(SymbolDeclaration.EnumValue("p.E1", "A"), SymbolDeclaration.EnumValue("p.E2", "A")),
                        ),
                    )
            }

            "rejects the same value name in two enums without a package" {
                resolve(enum("E1", "A"), enum("E2", "A")).issues() shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.SymbolNameCollision(
                            "",
                            "A",
                            listOf(SymbolDeclaration.EnumValue("E1", "A"), SymbolDeclaration.EnumValue("E2", "A")),
                        ),
                    )
            }

            "accepts the same value name in enums of different packages" {
                resolve(enum("x.E", "A"), enum("y.E", "A"))
                    .shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()
            }

            "rejects an enum value named like a message in the package" {
                resolve(message("p.Foo"), enum("p.E", "Foo")).issues() shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.SymbolNameCollision(
                            "p",
                            "Foo",
                            listOf(SymbolDeclaration.EnumValue("p.E", "Foo"), SymbolDeclaration.Type("p.Foo")),
                        ),
                    )
            }

            "rejects an enum value named like its own enum" {
                resolve(enum("Status", "Status")).issues() shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.SymbolNameCollision(
                            "",
                            "Status",
                            listOf(SymbolDeclaration.Type("Status"), SymbolDeclaration.EnumValue("Status", "Status")),
                        ),
                    )
            }

            "rejects an enum value named like a sub-package" {
                resolve(enum("a.E", "b"), message("a.b.C")).issues() shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.SymbolNameCollision(
                            "a",
                            "b",
                            listOf(SymbolDeclaration.EnumValue("a.E", "b"), SymbolDeclaration.Package("a.b")),
                        ),
                    )
            }

            "rejects a message named like a sub-package" {
                resolve(message("a.b"), message("a.b.C")).issues() shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.SymbolNameCollision(
                            "a",
                            "b",
                            listOf(SymbolDeclaration.Type("a.b"), SymbolDeclaration.Package("a.b")),
                        ),
                    )
            }

            "rejects an enum value named like a declaration of another dialect in the package" {
                val service =
                    ProtobufSchema(
                        "p.S",
                        object : ServiceDeclaration {
                            override val name = "S"
                        },
                    )

                resolve(enum("p.E", "S"), service).issues() shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.SymbolNameCollision(
                            "p",
                            "S",
                            listOf(SymbolDeclaration.EnumValue("p.E", "S"), SymbolDeclaration.Type("p.S")),
                        ),
                    )
            }

            "reports duplicate field names only once" {
                val schema = message("M", listOf(string("a", 1), string("a", 2)))

                resolve(schema).issues() shouldContainExactly
                    listOf(ProtobufResolutionIssue.DuplicateFieldNames("M", listOf("a")))
            }

            "reports duplicate oneof names only once" {
                val schema = message("M", oneofs = listOf(oneof("o", "a" to 1), oneof("o", "b" to 2)))

                resolve(schema).issues() shouldContainExactly
                    listOf(ProtobufResolutionIssue.DuplicateOneofNames("M", listOf("o")))
            }

            "reports duplicate enum value names within one enum only once" {
                val schema = ProtobufSchema("E", Enum("E", listOf(EnumValue("A", 0), EnumValue("A", 1))))

                resolve(schema).issues() shouldContainExactly
                    listOf(ProtobufResolutionIssue.DuplicateEnumValueNames("E", listOf("A")))
            }

            "reports duplicate map field names only once" {
                val schema = ProtobufSchema("M", Message("M", listOf(mapField("foo", 1), mapField("foo", 2))))

                resolve(schema).issues() shouldContainExactly
                    listOf(ProtobufResolutionIssue.DuplicateFieldNames("M", listOf("foo")))
            }

            "reports collisions after the issues of each schema in a stable order" {
                val issues =
                    resolve(
                            enum("p.E2", "A"),
                            message("p.Broken", listOf(string("a", 1), string("a", 2))),
                            enum("p.E1", "A"),
                        )
                        .issues()

                issues shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.DuplicateFieldNames("p.Broken", listOf("a")),
                        ProtobufResolutionIssue.SymbolNameCollision(
                            "p",
                            "A",
                            listOf(SymbolDeclaration.EnumValue("p.E1", "A"), SymbolDeclaration.EnumValue("p.E2", "A")),
                        ),
                    )
            }

            "does not report a collision for a field and a oneof whose invalid names are equal" {
                val schema = message("M", listOf(string("a b", 1)), listOf(oneof("a b", "c" to 2)))

                resolve(schema).issues() shouldContainExactlyInAnyOrder
                    listOf(
                        ProtobufResolutionIssue.InvalidIdentifier(
                            "M",
                            ProtobufResolutionIssue.IdentifierLocation.Field("a b"),
                            "a b",
                        ),
                        ProtobufResolutionIssue.InvalidIdentifier(
                            "M",
                            ProtobufResolutionIssue.IdentifierLocation.Oneof("a b"),
                            "a b",
                        ),
                    )
            }
        },
    )
