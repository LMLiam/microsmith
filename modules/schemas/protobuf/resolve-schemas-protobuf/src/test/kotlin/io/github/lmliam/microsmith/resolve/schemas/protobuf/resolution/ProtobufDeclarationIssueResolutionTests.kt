package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import io.github.lmliam.microsmith.dsl.schemas.SchemasExtension
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MessageField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.OneofField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.EnumValue
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.types.shouldBeTypeOf

class ProtobufDeclarationIssueResolutionTests :
    StringSpec(
        {
            val resolver = ProtobufSchemasResolver()

            fun issuesOf(schemaName: String, declaration: Type) =
                resolver
                    .resolve(SchemasExtension(setOf(ProtobufSchema(schemaName, declaration))))
                    .shouldBeTypeOf<DomainResolution.Failure>()
                    .issues
                    .toList()

            fun message(
                fields: List<MessageField> = emptyList(),
                oneofs: List<Oneof> = emptyList(),
                reserved: List<Reserved> = emptyList(),
            ) = Message("M", fields, oneofs, reserved)

            fun enum(vararg values: Pair<String, Int>, reserved: List<Reserved> = emptyList()) =
                Enum("E", values.map { EnumValue(it.first, it.second) }, reserved)

            fun string(name: String, number: Int) = ScalarField(name, number, PrimitiveType.STRING)

            fun mapField(name: String, number: Int) =
                MapField(name, number, MapType(PrimitiveType.STRING, PrimitiveType.STRING))

            fun oneof(name: String, vararg fields: Pair<String, Int>) =
                Oneof(name, fields.map { OneofField(it.first, it.second, PrimitiveType.STRING) })

            "rejects an enum without values" {
                issuesOf("E", enum()) shouldContainExactly listOf(ProtobufResolutionIssue.EmptyEnum("E"))
            }

            "rejects a oneof without fields" {
                issuesOf("M", message(oneofs = listOf(oneof("choice")))) shouldContainExactly
                    listOf(ProtobufResolutionIssue.EmptyOneof("M", "choice"))
            }

            "rejects every oneof without fields" {
                issuesOf("M", message(oneofs = listOf(oneof("first"), oneof("second")))) shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.EmptyOneof("M", "first"),
                        ProtobufResolutionIssue.EmptyOneof("M", "second"),
                    )
            }

            "reports an invalid oneof name before the oneof being empty" {
                issuesOf("M", message(oneofs = listOf(oneof("1choice")))) shouldContainExactly
                    listOf(
                        ProtobufResolutionIssue.InvalidIdentifier(
                            "M",
                            ProtobufResolutionIssue.IdentifierLocation.Oneof("1choice"),
                            "1choice",
                        ),
                        ProtobufResolutionIssue.EmptyOneof("M", "1choice"),
                    )
            }

            "rejects a field name that a oneof field repeats" {
                val declaration = message(fields = listOf(string("a", 1)), oneofs = listOf(oneof("choice", "a" to 2)))

                issuesOf("M", declaration) shouldContainExactly
                    listOf(ProtobufResolutionIssue.DuplicateFieldNames("M", listOf("a")))
            }

            "rejects a field name repeated by oneof fields of different oneofs" {
                val declaration = message(oneofs = listOf(oneof("first", "a" to 1), oneof("second", "a" to 2)))

                issuesOf("M", declaration) shouldContainExactly
                    listOf(ProtobufResolutionIssue.DuplicateFieldNames("M", listOf("a")))
            }

            "rejects a field name that a map field repeats" {
                issuesOf("M", message(fields = listOf(string("a", 1), mapField("a", 2)))) shouldContainExactly
                    listOf(ProtobufResolutionIssue.DuplicateFieldNames("M", listOf("a")))
            }

            "rejects a field number that a oneof field repeats" {
                val declaration = message(fields = listOf(string("a", 1)), oneofs = listOf(oneof("choice", "b" to 1)))

                issuesOf("M", declaration) shouldContainExactly
                    listOf(ProtobufResolutionIssue.DuplicateFieldNumbers("M", listOf(1)))
            }

            "rejects a field number that a map field repeats" {
                issuesOf("M", message(fields = listOf(string("a", 1), mapField("b", 1)))) shouldContainExactly
                    listOf(ProtobufResolutionIssue.DuplicateFieldNumbers("M", listOf(1)))
            }

            "rejects a reserved name that a oneof field uses" {
                val declaration =
                    message(oneofs = listOf(oneof("choice", "legacy" to 1)), reserved = listOf(ReservedName("legacy")))

                issuesOf("M", declaration) shouldContainExactly
                    listOf(ProtobufResolutionIssue.ReservedNameCollision("M", listOf("legacy")))
            }

            "rejects a reserved name that a map field uses" {
                val declaration =
                    message(fields = listOf(mapField("legacy", 1)), reserved = listOf(ReservedName("legacy")))

                issuesOf("M", declaration) shouldContainExactly
                    listOf(ProtobufResolutionIssue.ReservedNameCollision("M", listOf("legacy")))
            }

            "rejects a reserved number that a oneof field uses" {
                val declaration =
                    message(oneofs = listOf(oneof("choice", "a" to 5)), reserved = listOf(ReservedIndex(5)))

                issuesOf("M", declaration) shouldContainExactly
                    listOf(ProtobufResolutionIssue.ReservedNumberCollision("M", listOf(5)))
            }

            "rejects a reserved number that a map field uses" {
                val declaration = message(fields = listOf(mapField("a", 5)), reserved = listOf(ReservedRange(1..9)))

                issuesOf("M", declaration) shouldContainExactly
                    listOf(ProtobufResolutionIssue.ReservedNumberCollision("M", listOf(5)))
            }

            "rejects an enum reserved name that a value uses" {
                val declaration = enum("UNSPECIFIED" to 0, "ACTIVE" to 1, reserved = listOf(ReservedName("ACTIVE")))

                issuesOf("E", declaration) shouldContainExactly
                    listOf(ProtobufResolutionIssue.ReservedNameCollision("E", listOf("ACTIVE")))
            }

            "rejects an enum reserved number that a value uses" {
                val declaration = enum("UNSPECIFIED" to 0, "ACTIVE" to 5, reserved = listOf(ReservedRange(1..9)))

                issuesOf("E", declaration) shouldContainExactly
                    listOf(ProtobufResolutionIssue.ReservedNumberCollision("E", listOf(5)))
            }

            "rejects an enum reservation to max that overlaps a reserved number above the field number limit" {
                val declaration =
                    enum("UNSPECIFIED" to 0, reserved = listOf(ReservedToMax(1), ReservedIndex(Int.MAX_VALUE)))

                issuesOf("E", declaration) shouldContainExactly
                    listOf(ProtobufResolutionIssue.OverlappingReservedNumbers("E"))
            }

            "rejects an enum reservation to max that covers a used number above the field number limit" {
                val declaration = enum("UNSPECIFIED" to 0, "HIGH" to Int.MAX_VALUE, reserved = listOf(ReservedToMax(1)))

                issuesOf("E", declaration) shouldContainExactly
                    listOf(ProtobufResolutionIssue.ReservedNumberCollision("E", listOf(Int.MAX_VALUE)))
            }
        },
    )
