package io.github.lmliam.microsmith.resolve.schemas.protobuf.parsing

import arrow.core.raise.Raise
import arrow.core.raise.context.bind
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.FieldNumber

context(_: Raise<ProtobufResolutionIssue>)
internal fun parseIdentifier(
    schemaName: String,
    location: ProtobufResolutionIssue.IdentifierLocation,
    raw: String,
): ProtoIdentifier =
    ProtoIdentifier.parse(raw).mapLeft { ProtobufResolutionIssue.InvalidIdentifier(schemaName, location, it) }.bind()

context(_: Raise<ProtobufResolutionIssue>)
internal fun parseFieldNumber(
    schemaName: String,
    location: ProtobufResolutionIssue.FieldLocation,
    raw: Int,
): FieldNumber =
    FieldNumber.parse(raw).mapLeft { ProtobufResolutionIssue.InvalidFieldNumber(schemaName, location, it) }.bind()
