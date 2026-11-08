package io.github.lmliam.microsmith.resolve.schemas.protobuf

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Cardinality
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.FieldNumber
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ResolvedProtobufMessageTests :
    StringSpec({
        fun identifier(value: String) =
            ProtoIdentifier.parse(value).getOrElse { error("'$value' is not an identifier") }

        fun number(value: Int) = FieldNumber.parse(value).getOrElse { error("$value is not a field number") }

        fun scalar(name: String, number: Int): ResolvedProtobufField =
            ResolvedProtobufField.Scalar(identifier(name), number(number), PrimitiveType.STRING, Cardinality.SINGULAR)

        fun map(name: String, number: Int): ResolvedProtobufField =
            ResolvedProtobufField.Map(
                identifier(name),
                number(number),
                PrimitiveType.STRING,
                ResolvedProtobufValueType.Primitive(PrimitiveType.STRING),
            )

        fun oneofField(field: Pair<String, Int>) =
            ResolvedProtobufOneof.Field(
                identifier(field.first),
                number(field.second),
                ResolvedProtobufValueType.Primitive(PrimitiveType.STRING),
            )

        fun oneof(name: String, first: Pair<String, Int>, vararg rest: Pair<String, Int>) =
            ResolvedProtobufOneof(
                identifier(name),
                nonEmptyListOf(oneofField(first), *rest.map(::oneofField).toTypedArray()),
            )

        fun reservations(
            names: List<String> = emptyList(),
            spans: List<ReservedSpan> = emptyList(),
        ): ResolvedProtobufReservations =
            ResolvedProtobufReservations.of(names.map(::identifier), spans).getOrElse { error("invalid: $it") }

        fun span(first: Int, last: Int, domain: ReservedSpan.Domain = ReservedSpan.Domain.MESSAGE) =
            ReservedSpan.range(first, last, domain).getOrElse { error("$first..$last is not reservable") }

        fun messageOf(
            fields: List<ResolvedProtobufField> = emptyList(),
            oneofs: List<ResolvedProtobufOneof> = emptyList(),
            reservations: ResolvedProtobufReservations = ResolvedProtobufReservations.NONE,
        ) = ResolvedProtobufMessage.of(identifier("Contact"), fields, oneofs, reservations)

        fun rejection(first: ResolvedProtobufRejection, vararg rest: ResolvedProtobufRejection) =
            Either.Left(nonEmptyListOf(first, *rest))

        "keeps its fields and oneofs in the order given" {
            val message =
                messageOf(
                        fields = listOf(scalar("b", 2), scalar("a", 1)),
                        oneofs = listOf(oneof("channel", "email" to 3, "phone" to 4)),
                    )
                    .getOrElse { error("expected a message, got $it") }

            message.fields.map { it.name.value } shouldContainExactly listOf("b", "a")
            message.oneofs.map { it.name.value } shouldContainExactly listOf("channel")
        }

        "keeps the fields and oneofs it accepted when the given lists change afterwards" {
            val fields = mutableListOf(scalar("a", 1))
            val oneofs = mutableListOf(oneof("channel", "b" to 2))
            val message =
                ResolvedProtobufMessage.of(identifier("Contact"), fields, oneofs, ResolvedProtobufReservations.NONE)
                    .getOrElse { error("expected a message, got $it") }

            fields.add(scalar("a", 1))
            oneofs.add(oneof("channel", "b" to 2))

            message.fields.map { it.name.value } shouldContainExactly listOf("a")
            message.oneofs.map { it.name.value } shouldContainExactly listOf("channel")
        }

        "rejects a field name that a oneof field repeats" {
            messageOf(fields = listOf(scalar("a", 1)), oneofs = listOf(oneof("channel", "a" to 2))) shouldBe
                rejection(ResolvedProtobufRejection.DuplicateFieldNames(listOf("a")))
        }

        "rejects a field name that a map field repeats" {
            messageOf(fields = listOf(scalar("a", 1), map("a", 2))) shouldBe
                rejection(ResolvedProtobufRejection.DuplicateFieldNames(listOf("a")))
        }

        "rejects a field number that a oneof field repeats" {
            messageOf(fields = listOf(scalar("a", 1)), oneofs = listOf(oneof("channel", "b" to 1))) shouldBe
                rejection(ResolvedProtobufRejection.DuplicateFieldNumbers(listOf(1)))
        }

        "rejects a field number that a map field repeats" {
            messageOf(fields = listOf(scalar("a", 1), map("b", 1))) shouldBe
                rejection(ResolvedProtobufRejection.DuplicateFieldNumbers(listOf(1)))
        }

        "rejects two oneofs with the same name" {
            messageOf(oneofs = listOf(oneof("channel", "a" to 1), oneof("channel", "b" to 2))) shouldBe
                rejection(ResolvedProtobufRejection.DuplicateOneofNames(listOf("channel")))
        }

        "rejects a oneof field whose name is reserved" {
            messageOf(
                oneofs = listOf(oneof("channel", "legacy" to 1)),
                reservations = reservations(names = listOf("legacy")),
            ) shouldBe rejection(ResolvedProtobufRejection.ReservedNameCollision(listOf("legacy")))
        }

        "rejects a map field whose number is reserved" {
            messageOf(fields = listOf(map("a", 5)), reservations = reservations(spans = listOf(span(1, 9)))) shouldBe
                rejection(ResolvedProtobufRejection.ReservedNumberCollision(listOf(5)))
        }

        "rejects a reserved span written for enum values" {
            messageOf(reservations = reservations(spans = listOf(span(1, 5, ReservedSpan.Domain.ENUM)))) shouldBe
                rejection(
                    ResolvedProtobufRejection.ReservedSpanDomainMismatch("1 to 5", 1, ReservedSpan.Domain.MESSAGE)
                )
        }

        "rejects field names that share a JSON name" {
            messageOf(fields = listOf(scalar("foo_bar", 1), scalar("fooBar", 2))) shouldBe
                rejection(ResolvedProtobufRejection.FieldJsonNameCollision("fooBar", listOf("foo_bar", "fooBar")))
        }

        "rejects a oneof field and a map field that share a JSON name" {
            messageOf(fields = listOf(map("foo_bar", 1)), oneofs = listOf(oneof("channel", "fooBar" to 2))) shouldBe
                rejection(ResolvedProtobufRejection.FieldJsonNameCollision("fooBar", listOf("foo_bar", "fooBar")))
        }

        "reports each JSON name that several field names share" {
            messageOf(fields = listOf(scalar("a_b", 1), scalar("aB", 2), scalar("c_d", 3), scalar("cD", 4))) shouldBe
                rejection(
                    ResolvedProtobufRejection.FieldJsonNameCollision("aB", listOf("a_b", "aB")),
                    ResolvedProtobufRejection.FieldJsonNameCollision("cD", listOf("c_d", "cD")),
                )
        }

        "rejects a oneof named like a field" {
            messageOf(fields = listOf(scalar("x", 1)), oneofs = listOf(oneof("x", "y" to 2))) shouldBe
                rejection(
                    ResolvedProtobufRejection.MessageSymbolCollision(
                        "x",
                        listOf(
                            ResolvedProtobufRejection.MessageSymbol.Field("x"),
                            ResolvedProtobufRejection.MessageSymbol.Oneof("x"),
                        ),
                    )
                )
        }

        "rejects a field named like the entry message of a map field" {
            messageOf(fields = listOf(map("foo", 1), scalar("FooEntry", 2))) shouldBe
                rejection(
                    ResolvedProtobufRejection.MessageSymbolCollision(
                        "FooEntry",
                        listOf(
                            ResolvedProtobufRejection.MessageSymbol.Field("FooEntry"),
                            ResolvedProtobufRejection.MessageSymbol.MapEntry("foo"),
                        ),
                    )
                )
        }

        "rejects a oneof named like the entry message of a map field" {
            messageOf(fields = listOf(map("foo", 1)), oneofs = listOf(oneof("FooEntry", "y" to 2))) shouldBe
                rejection(
                    ResolvedProtobufRejection.MessageSymbolCollision(
                        "FooEntry",
                        listOf(
                            ResolvedProtobufRejection.MessageSymbol.Oneof("FooEntry"),
                            ResolvedProtobufRejection.MessageSymbol.MapEntry("foo"),
                        ),
                    )
                )
        }

        "rejects two map fields that synthesise the same entry message name" {
            messageOf(fields = listOf(map("foo_bar", 1), map("fooBar", 2))) shouldBe
                rejection(
                    ResolvedProtobufRejection.FieldJsonNameCollision("fooBar", listOf("foo_bar", "fooBar")),
                    ResolvedProtobufRejection.MessageSymbolCollision(
                        "FooBarEntry",
                        listOf(
                            ResolvedProtobufRejection.MessageSymbol.MapEntry("foo_bar"),
                            ResolvedProtobufRejection.MessageSymbol.MapEntry("fooBar"),
                        ),
                    ),
                )
        }

        "accepts a field whose name differs from a map entry name only by case" {
            messageOf(fields = listOf(map("foo", 1), scalar("fooEntry", 2))).isRight() shouldBe true
        }

        "reports every rejection in the order the resolver reports issues" {
            messageOf(
                fields = listOf(scalar("a", 1), scalar("a", 1)),
                oneofs = listOf(oneof("channel", "b" to 2), oneof("channel", "c" to 3)),
                reservations = reservations(names = listOf("a"), spans = listOf(span(1, 1))),
            ) shouldBe
                rejection(
                    ResolvedProtobufRejection.DuplicateFieldNames(listOf("a")),
                    ResolvedProtobufRejection.DuplicateFieldNumbers(listOf(1)),
                    ResolvedProtobufRejection.DuplicateOneofNames(listOf("channel")),
                    ResolvedProtobufRejection.ReservedNameCollision(listOf("a")),
                    ResolvedProtobufRejection.ReservedNumberCollision(listOf(1)),
                )
        }
    })
