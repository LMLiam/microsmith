package io.github.lmliam.microsmith.resolve.schemas.protobuf.parsing

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufRejection
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ProtobufRejectionIssuesTests :
    StringSpec(
        {
            "reports a JSON name collision under the schema that was being sealed" {
                ResolvedProtobufRejection.FieldJsonNameCollision("fooBar", listOf("foo_bar", "fooBar"))
                    .toIssue("a.Contact") shouldBe
                    ProtobufResolutionIssue.FieldJsonNameCollision("a.Contact", "fooBar", listOf("foo_bar", "fooBar"))
            }

            "reports an enum value name collision under the schema that was being sealed" {
                ResolvedProtobufRejection.EnumValueNameCollision(listOf("STATUS_ACTIVE", "ACTIVE"))
                    .toIssue("a.Status") shouldBe
                    ProtobufResolutionIssue.EnumValueNameCollision("a.Status", listOf("STATUS_ACTIVE", "ACTIVE"))
            }

            "qualifies the members of a message symbol collision with the schema that was being sealed" {
                ResolvedProtobufRejection.MessageSymbolCollision(
                        "FooEntry",
                        listOf(
                            ResolvedProtobufRejection.MessageSymbol.Field("FooEntry"),
                            ResolvedProtobufRejection.MessageSymbol.Oneof("FooEntry"),
                            ResolvedProtobufRejection.MessageSymbol.MapEntry("foo"),
                        ),
                    )
                    .toIssue("a.Contact") shouldBe
                    ProtobufResolutionIssue.SymbolNameCollision(
                        "a.Contact",
                        "FooEntry",
                        listOf(
                            ProtobufResolutionIssue.SymbolDeclaration.Field("a.Contact", "FooEntry"),
                            ProtobufResolutionIssue.SymbolDeclaration.Oneof("a.Contact", "FooEntry"),
                            ProtobufResolutionIssue.SymbolDeclaration.MapEntry("a.Contact", "foo"),
                        ),
                    )
            }
        },
    )
