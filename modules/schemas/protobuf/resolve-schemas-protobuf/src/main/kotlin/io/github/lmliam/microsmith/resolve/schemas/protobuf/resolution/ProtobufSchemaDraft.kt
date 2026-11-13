package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import arrow.core.EitherNel
import arrow.core.NonEmptyList
import io.github.lmliam.microsmith.resolve.schemas.protobuf.LocatedReference
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufDeclaration
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnumValue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufField
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufOneof
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufRejection
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufReservations
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchema
import io.github.lmliam.microsmith.resolve.schemas.protobuf.dependenciesOf
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.referencesOf
import io.github.lmliam.microsmith.resolve.schemas.protobuf.targets
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.SchemaNode

internal data class SchemaDraft(val identity: QualifiedSchemaName, val declaration: DeclarationDraft) {
    val node: SchemaNode
        get() = SchemaNode(identity, dependenciesOf(identity, declaration.references().targets()))

    fun seal(): EitherNel<ResolvedProtobufRejection, ResolvedProtobufSchema> =
        declaration.seal().map { ResolvedProtobufSchema.of(it, packageOf = identity) }
}

internal sealed interface DeclarationDraft {
    fun references(): List<LocatedReference>

    fun seal(): EitherNel<ResolvedProtobufRejection, ResolvedProtobufDeclaration>

    data class OfMessage(
        val name: ProtoIdentifier,
        val fields: List<ResolvedProtobufField>,
        val oneofs: List<ResolvedProtobufOneof>,
        val reservations: ResolvedProtobufReservations,
    ) : DeclarationDraft {
        override fun references() = referencesOf(fields, oneofs)

        override fun seal() = ResolvedProtobufMessage.of(name, fields, oneofs, reservations)
    }

    data class OfEnum(
        val name: ProtoIdentifier,
        val values: NonEmptyList<ResolvedProtobufEnumValue>,
        val reservations: ResolvedProtobufReservations,
    ) : DeclarationDraft {
        override fun references() = emptyList<LocatedReference>()

        override fun seal() = ResolvedProtobufEnum.of(name, values, reservations)
    }
}
