package io.github.lmliam.microsmith.resolve.schemas.protobuf

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Cardinality
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.FieldNumber
import io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution.ProtobufDeclarationKind
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ResolvedProtobufSchemaModelTests :
    StringSpec({
        fun name(value: String) = QualifiedSchemaName.parse(value).getOrElse { error("'$value' is not a name") }

        fun schema(fullName: String, vararg targets: String): ResolvedProtobufSchema {
            val identity = name(fullName)
            val fields = targets.mapIndexed { index, target ->
                ResolvedProtobufField.Reference(
                    ProtoIdentifier.parse("to$index").getOrElse { error("not an identifier") },
                    FieldNumber.parse(index + 1).getOrElse { error("not a field number") },
                    ResolvedProtobufReference(name(target), ProtobufDeclarationKind.MESSAGE),
                    Cardinality.SINGULAR,
                )
            }
            val message =
                ResolvedProtobufMessage.of(identity.typeName, fields, emptyList(), ResolvedProtobufReservations.NONE)
                    .getOrElse { error("invalid message: $it") }

            return ResolvedProtobufSchema.of(message, packageOf = identity)
        }

        fun cycle(vararg schemas: String) = ResolvedProtobufSchemaModel.Rejection.CircularImports(schemas.toList())

        "keeps its schemas in the order given" {
            val model =
                ResolvedProtobufSchemaModel.of(listOf(schema("B"), schema("A", "B"))).getOrElse {
                    error("expected a model, got $it")
                }

            model.schemas.map { it.identity.fullyQualifiedName } shouldContainExactly listOf("B", "A")
        }

        "keeps the schemas it accepted when the given list changes afterwards" {
            val schemas = mutableListOf(schema("A"))
            val model = ResolvedProtobufSchemaModel.of(schemas).getOrElse { error("expected a model, got $it") }

            schemas.add(schema("A"))

            model.schemas.map { it.identity.fullyQualifiedName } shouldContainExactly listOf("A")
        }

        "rejects a schema identity that appears more than once" {
            ResolvedProtobufSchemaModel.of(listOf(schema("a.A"), schema("B"), schema("a.A", "B"))) shouldBe
                Either.Left(nonEmptyListOf(ResolvedProtobufSchemaModel.Rejection.DuplicateSchema(name("a.A"), 2)))
        }

        "accepts schemas that reference each other in one direction only" {
            ResolvedProtobufSchemaModel.of(listOf(schema("A", "B", "C"), schema("B", "C"), schema("C")))
                .isRight() shouldBe true
        }

        "accepts a schema that references itself" {
            ResolvedProtobufSchemaModel.of(listOf(schema("A", "A"))).isRight() shouldBe true
        }

        "rejects two schemas that reference each other" {
            ResolvedProtobufSchemaModel.of(listOf(schema("B", "A"), schema("A", "B"))) shouldBe
                Either.Left(nonEmptyListOf(cycle("A", "B")))
        }

        "reports each independent cycle once in name order" {
            ResolvedProtobufSchemaModel.of(
                listOf(schema("D", "C"), schema("C", "D"), schema("B", "A"), schema("A", "B"))
            ) shouldBe Either.Left(nonEmptyListOf(cycle("A", "B"), cycle("C", "D")))
        }
    })
