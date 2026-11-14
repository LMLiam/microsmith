package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ResolvedProtobufRpcSchemaTests :
    StringSpec(
        {
            fun identifier(value: String) =
                ProtoIdentifier.parse(value).getOrElse { error("'$value' is not an identifier") }

            fun name(value: String) = QualifiedSchemaName.parse(value).getOrElse { error("'$value' is not a name") }

            fun endpoint(target: String) = ResolvedProtobufRpcEndpoint(name(target), streaming = false)

            fun rpc(rpcName: String, request: String, response: String) =
                ResolvedProtobufRpc(identifier(rpcName), endpoint(request), endpoint(response))

            "accepts a service without rpcs" {
                val schema =
                    ResolvedProtobufRpcSchema.of(name("acme.UserService"), emptyList()).getOrElse {
                        error("rejected: $it")
                    }

                schema.rpcs.shouldBeEmpty()
                schema.dependencies.shouldBeEmpty()
            }

            "rejects an rpc name declared twice" {
                val result =
                    ResolvedProtobufRpcSchema.of(
                        name("acme.UserService"),
                        listOf(
                            rpc("GetUser", "acme.Request", "acme.Response"),
                            rpc("GetUser", "acme.OtherRequest", "acme.OtherResponse"),
                        ),
                    )

                result.leftOrNull() shouldBe
                    nonEmptyListOf(ResolvedProtobufRpcSchema.Rejection.DuplicateRpcName(identifier("GetUser")))
            }

            "reports each duplicated rpc name once" {
                val result =
                    ResolvedProtobufRpcSchema.of(
                        name("acme.UserService"),
                        listOf(
                            rpc("A", "acme.Request", "acme.Response"),
                            rpc("A", "acme.Request", "acme.Response"),
                            rpc("A", "acme.Request", "acme.Response"),
                            rpc("B", "acme.Request", "acme.Response"),
                            rpc("B", "acme.Request", "acme.Response"),
                        ),
                    )

                result.leftOrNull()?.toList() shouldContainExactly
                    listOf(
                        ResolvedProtobufRpcSchema.Rejection.DuplicateRpcName(identifier("A")),
                        ResolvedProtobufRpcSchema.Rejection.DuplicateRpcName(identifier("B")),
                    )
            }

            "derives sorted distinct dependencies from the endpoint targets" {
                val schema =
                    ResolvedProtobufRpcSchema.of(
                            name("acme.user.UserService"),
                            listOf(
                                rpc("GetUser", "acme.user.Request", "acme.shared.Response"),
                                rpc("ListUsers", "acme.user.Request", "acme.shared.Response"),
                                rpc("Ping", "acme.user.Request", "acme.user.Reply"),
                            ),
                        )
                        .getOrElse { error("rejected: $it") }

                schema.dependencies.map { it.fullyQualifiedName } shouldContainExactly
                    listOf("acme.shared.Response", "acme.user.Reply", "acme.user.Request")
            }

            "does not depend on itself" {
                val schema =
                    ResolvedProtobufRpcSchema.of(
                            name("acme.UserService"),
                            listOf(rpc("Echo", "acme.UserService", "acme.UserService")),
                        )
                        .getOrElse { error("rejected: $it") }

                schema.dependencies.shouldBeEmpty()
            }
        },
    )
