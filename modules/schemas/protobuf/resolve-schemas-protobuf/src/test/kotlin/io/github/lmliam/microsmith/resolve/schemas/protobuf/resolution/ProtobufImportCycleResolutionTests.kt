package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import io.github.lmliam.microsmith.dsl.schemas.SchemasExtension
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MessageField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.OneofField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.types.shouldBeTypeOf

class ProtobufImportCycleResolutionTests :
    StringSpec(
        {
            val resolver = ProtobufSchemasResolver()

            fun resolve(schemas: List<ProtobufSchema>) = resolver.resolve(SchemasExtension(schemas.toSet()))

            fun DomainResolution<*>.issues() = shouldBeTypeOf<DomainResolution.Failure>().issues.toList()

            fun referencing(schemaName: String, vararg targets: String) =
                ProtobufSchema(
                    schemaName,
                    Message(
                        schemaName.substringAfterLast('.'),
                        targets.mapIndexed { index, target ->
                            ReferenceField("to$index", index + 1, Reference.Qualified(target)) as MessageField
                        },
                    ),
                )

            fun cycle(vararg schemas: String) = ProtobufResolutionIssue.CircularSchemaImports(schemas.toList())

            "rejects two schemas that reference each other" {
                resolve(listOf(referencing("A", "B"), referencing("B", "A"))).issues() shouldContainExactly
                    listOf(cycle("A", "B"))
            }

            "rejects a cycle across packages" {
                resolve(listOf(referencing("x.A", "y.B"), referencing("y.B", "x.A"))).issues() shouldContainExactly
                    listOf(cycle("x.A", "y.B"))
            }

            "reports a cycle of three schemas once starting at the smallest name" {
                val schemas = listOf(referencing("C", "A"), referencing("A", "B"), referencing("B", "C"))

                resolve(schemas).issues() shouldContainExactly listOf(cycle("A", "B", "C"))
            }

            "reports the same cycle regardless of the order the schemas are declared in" {
                val schemas = listOf(referencing("B", "C"), referencing("C", "A"), referencing("A", "B"))

                resolve(schemas.reversed()).issues() shouldContainExactly resolve(schemas).issues()
            }

            "reports each independent cycle once in name order" {
                val schemas =
                    listOf(
                        referencing("D", "C"),
                        referencing("C", "D"),
                        referencing("B", "A"),
                        referencing("A", "B"),
                    )

                resolve(schemas).issues() shouldContainExactly listOf(cycle("A", "B"), cycle("C", "D"))
            }

            "reports one cycle for a group of schemas that import each other through several paths" {
                val schemas = listOf(referencing("A", "B"), referencing("B", "A", "C"), referencing("C", "B"))

                resolve(schemas).issues() shouldContainExactly listOf(cycle("A", "B"))
            }

            "detects a cycle that goes through a map value" {
                val schemas =
                    listOf(
                        ProtobufSchema(
                            "A",
                            Message(
                                "A",
                                listOf(MapField("by", 1, MapType(PrimitiveType.STRING, Reference.Qualified("B")))),
                            ),
                        ),
                        referencing("B", "A"),
                    )

                resolve(schemas).issues() shouldContainExactly listOf(cycle("A", "B"))
            }

            "detects a cycle that goes through a oneof field" {
                val schemas =
                    listOf(
                        ProtobufSchema(
                            "A",
                            Message(
                                "A",
                                oneofs = listOf(Oneof("choice", listOf(OneofField("b", 1, Reference.Qualified("B"))))),
                            ),
                        ),
                        referencing("B", "A"),
                    )

                resolve(schemas).issues() shouldContainExactly listOf(cycle("A", "B"))
            }

            "accepts a schema that references itself" {
                resolve(listOf(referencing("A", "A")))
                    .shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()
            }

            "accepts schemas that reference each other in one direction only" {
                resolve(listOf(referencing("A", "B", "C"), referencing("B", "C"), referencing("C")))
                    .shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()
            }

            "reports a cycle after the issues of each schema" {
                val broken =
                    ProtobufSchema("Broken", Message("Broken", listOf(ScalarField("a", 0, PrimitiveType.STRING))))
                val schemas = listOf(referencing("A", "B"), referencing("B", "A"), broken)

                resolve(schemas).issues() shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.InvalidFieldNumber(
                            "Broken",
                            ProtobufResolutionIssue.FieldLocation.Field("a"),
                            0,
                        ),
                        cycle("A", "B"),
                    )
            }

            "does not report a cycle through a schema that fails to resolve" {
                val broken =
                    ProtobufSchema(
                        "B",
                        Message(
                            "B",
                            listOf(
                                ReferenceField("to", 1, Reference.Qualified("A")),
                                ScalarField("a", 0, PrimitiveType.STRING),
                            ),
                        ),
                    )

                resolve(listOf(referencing("A", "B"), broken)).issues() shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.InvalidFieldNumber(
                            "B",
                            ProtobufResolutionIssue.FieldLocation.Field("a"),
                            0,
                        ),
                    )
            }
        },
    )
