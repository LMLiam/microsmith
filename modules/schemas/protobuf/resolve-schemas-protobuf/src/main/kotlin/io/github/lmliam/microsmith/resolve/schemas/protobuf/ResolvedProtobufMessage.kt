package io.github.lmliam.microsmith.resolve.schemas.protobuf

import arrow.core.EitherNel
import arrow.core.raise.context.either
import arrow.core.raise.context.zipOrAccumulate
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkNoReservedNameUse
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkNoReservedNumberUse
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkSpansBelongTo
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkUniqueFieldNames
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkUniqueFieldNumbers
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkUniqueOneofNames
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.findJsonNameCollisions
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.findMessageSymbolCollisions
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.rejectAll

@ConsistentCopyVisibility
public data class ResolvedProtobufMessage
private constructor(
    override val name: ProtoIdentifier,
    public val fields: List<ResolvedProtobufField>,
    public val oneofs: List<ResolvedProtobufOneof>,
    public val reservations: ResolvedProtobufReservations,
) : ResolvedProtobufDeclaration {
    public companion object {
        public fun of(
            name: ProtoIdentifier,
            fields: List<ResolvedProtobufField>,
            oneofs: List<ResolvedProtobufOneof>,
            reservations: ResolvedProtobufReservations,
        ): EitherNel<ResolvedProtobufRejection, ResolvedProtobufMessage> = either {
            val ownFields = fields.toList()
            val ownOneofs = oneofs.toList()
            val members =
                ownFields.map { it.name.value to it.number.value } +
                    ownOneofs.flatMap { oneof -> oneof.fields.map { it.name.value to it.number.value } }
            val memberNames = members.map { it.first }
            val mapFieldNames = ownFields.filterIsInstance<ResolvedProtobufField.Map>().map { it.name.value }
            val memberNumbers = members.map { it.second }

            zipOrAccumulate(
                { checkUniqueFieldNames(memberNames) },
                { checkUniqueFieldNumbers(memberNumbers) },
                { checkUniqueOneofNames(ownOneofs.map { it.name.value }) },
                { checkNoReservedNameUse(reservations.names.map(ProtoIdentifier::value), memberNames) },
                { checkNoReservedNumberUse(reservations.spans.map(ReservedSpan::numbers), memberNumbers) },
                { checkSpansBelongTo(ReservedSpan.Domain.MESSAGE, reservations.spans) },
                { rejectAll(findJsonNameCollisions(memberNames)) },
                { rejectAll(findMessageSymbolCollisions(memberNames, ownOneofs.map { it.name.value }, mapFieldNames)) },
            ) { _, _, _, _, _, _, _, _ ->
                ResolvedProtobufMessage(name, ownFields, ownOneofs, reservations)
            }
        }
    }
}
