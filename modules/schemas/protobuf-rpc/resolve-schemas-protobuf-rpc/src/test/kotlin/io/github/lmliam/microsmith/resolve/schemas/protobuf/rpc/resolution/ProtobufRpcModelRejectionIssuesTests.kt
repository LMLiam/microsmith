package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.resolution

import arrow.core.getOrElse
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ProtobufRpcResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchema
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchemaModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ProtobufRpcModelRejectionIssuesTests :
    StringSpec({
        "reports a repeated rpc name under the service that declares it" {
            val name = ProtoIdentifier.parse("GetUser").getOrElse { error("'GetUser' is not an identifier") }

            ResolvedProtobufRpcSchema.Rejection.DuplicateRpcName(name).toIssue("UserService") shouldBe
                ProtobufRpcResolutionIssue.DuplicateRpcName(serviceName = "UserService", rpcName = "GetUser")
        }

        "reports a repeated service identity as a collision of its type declarations" {
            val identity = QualifiedSchemaName.parse("p.Contact").getOrElse { error("not a name") }

            ResolvedProtobufRpcSchemaModel.Rejection.DuplicateService(identity, 2).toIssue() shouldBe
                ProtobufRpcResolutionIssue.SchemaIssue(
                    ProtobufResolutionIssue.SymbolNameCollision(
                        "p",
                        "Contact",
                        listOf(
                            ProtobufResolutionIssue.SymbolDeclaration.Type("p.Contact"),
                            ProtobufResolutionIssue.SymbolDeclaration.Type("p.Contact"),
                        ),
                    )
                )
        }
    })
