package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import arrow.core.getOrElse
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ProtobufModelRejectionIssuesTests :
    StringSpec({
        fun name(value: String) = QualifiedSchemaName.parse(value).getOrElse { error("'$value' is not a name") }

        val location = ProtobufResolutionIssue.ReferenceLocation.Field("owner")

        "reports an unresolved reference with its owner, location and target" {
            ResolvedProtobufSchemaModel.Rejection.UnresolvedReference(name("a.Owner"), location, name("b.Target"))
                .toIssue() shouldBe ProtobufResolutionIssue.UnresolvedReference("a.Owner", location, "b.Target")
        }

        "reports a reference of the wrong kind as an unresolved reference" {
            ResolvedProtobufSchemaModel.Rejection.ReferenceKindMismatch(
                    name("a.Owner"),
                    location,
                    name("b.Target"),
                    referencedAs = ProtobufDeclarationKind.MESSAGE,
                    declaredAs = ProtobufDeclarationKind.ENUM,
                )
                .toIssue() shouldBe ProtobufResolutionIssue.UnresolvedReference("a.Owner", location, "b.Target")
        }

        "reports a package symbol collision under the package that holds the symbols" {
            val declarations =
                listOf(
                    ProtobufResolutionIssue.SymbolDeclaration.EnumValue("p.Status", "Contact"),
                    ProtobufResolutionIssue.SymbolDeclaration.Type("p.Contact"),
                )

            ResolvedProtobufSchemaModel.Rejection.PackageSymbolCollision("p", "Contact", declarations)
                .toIssue() shouldBe ProtobufResolutionIssue.SymbolNameCollision("p", "Contact", declarations)
        }

        "reports a repeated schema identity as a collision of its type declarations" {
            ResolvedProtobufSchemaModel.Rejection.DuplicateSchema(name("p.Contact"), 2).toIssue() shouldBe
                ProtobufResolutionIssue.SymbolNameCollision(
                    "p",
                    "Contact",
                    listOf(
                        ProtobufResolutionIssue.SymbolDeclaration.Type("p.Contact"),
                        ProtobufResolutionIssue.SymbolDeclaration.Type("p.Contact"),
                    ),
                )
        }
    })
