package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import arrow.core.getOrElse
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpc
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcEndpoint
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchema
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchemaModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.types.shouldBeInstanceOf

class ProtobufRpcArtifactContributorTests :
    StringSpec({
        fun name(value: String) = QualifiedSchemaName.parse(value).getOrElse { error("'$value' is not a name") }

        fun rpc(rpcName: String, request: String, response: String) =
            ResolvedProtobufRpc(
                ProtoIdentifier.parse(rpcName).getOrElse { error("not an identifier") },
                ResolvedProtobufRpcEndpoint(name(request), streaming = false),
                ResolvedProtobufRpcEndpoint(name(response), streaming = false),
            )

        "contributes the dependencies as import paths ordered by schema name" {
            val schema =
                ResolvedProtobufRpcSchema.of(
                        name("acme.user.UserService"),
                        listOf(
                            rpc("GetUser", "acme.user.Request", "acme.shared.Response"),
                            rpc("ListUsers", "acme.user.Reply", "acme.user.Request"),
                        ),
                    )
                    .getOrElse { error("rejected: $it") }
            val model = ResolvedProtobufRpcSchemaModel.of(listOf(schema)).getOrElse { error("rejected: $it") }

            val contribution =
                ProtobufRpcArtifactContributor()
                    .contribute(model)
                    .single()
                    .shouldBeInstanceOf<ProtobufRpcServiceContribution>()

            contribution.imports shouldContainExactly
                listOf("acme/shared/Response.proto", "acme/user/Reply.proto", "acme/user/Request.proto")
        }

        "contributes each rpc as an operation with its endpoint type names and streaming" {
            val streamingRpc =
                ResolvedProtobufRpc(
                    ProtoIdentifier.parse("Watch").getOrElse { error("not an identifier") },
                    ResolvedProtobufRpcEndpoint(name("acme.user.WatchRequest"), streaming = false),
                    ResolvedProtobufRpcEndpoint(name("acme.user.Event"), streaming = true),
                )
            val schema =
                ResolvedProtobufRpcSchema.of(name("acme.user.UserService"), listOf(streamingRpc)).getOrElse {
                    error("rejected: $it")
                }
            val model = ResolvedProtobufRpcSchemaModel.of(listOf(schema)).getOrElse { error("rejected: $it") }

            val contribution =
                ProtobufRpcArtifactContributor()
                    .contribute(model)
                    .single()
                    .shouldBeInstanceOf<ProtobufRpcServiceContribution>()

            contribution.operations shouldContainExactly
                listOf(
                    ProtobufRpcOperation(
                        name = "Watch",
                        request = ProtobufRpcEndpoint("acme.user.WatchRequest", streaming = false),
                        response = ProtobufRpcEndpoint("acme.user.Event", streaming = true),
                    )
                )
        }
    })
