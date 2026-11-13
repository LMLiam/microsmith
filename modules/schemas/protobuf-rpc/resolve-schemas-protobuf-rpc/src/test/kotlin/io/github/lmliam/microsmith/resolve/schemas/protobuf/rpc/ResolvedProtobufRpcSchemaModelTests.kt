package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ResolvedProtobufRpcSchemaModelTests :
    StringSpec({
        fun name(value: String) = QualifiedSchemaName.parse(value).getOrElse { error("'$value' is not a name") }

        fun service(fullName: String): ResolvedProtobufRpcSchema =
            ResolvedProtobufRpcSchema.of(name(fullName), emptyList()).getOrElse { error("rejected: $it") }

        "accepts services with distinct names" {
            val model =
                ResolvedProtobufRpcSchemaModel.of(listOf(service("acme.A"), service("acme.B"), service("other.A")))
                    .getOrElse { error("rejected: $it") }

            model.schemas.map { it.qualifiedName.fullyQualifiedName } shouldContainExactly
                listOf("acme.A", "acme.B", "other.A")
        }

        "accepts a model without services" { ResolvedProtobufRpcSchemaModel.of(emptyList()).isRight() shouldBe true }

        "rejects two services with the same fully qualified name" {
            val result =
                ResolvedProtobufRpcSchemaModel.of(listOf(service("acme.A"), service("acme.B"), service("acme.A")))

            result.leftOrNull() shouldBe
                nonEmptyListOf(ResolvedProtobufRpcSchemaModel.Rejection.DuplicateService(name("acme.A"), 2))
        }
    })
