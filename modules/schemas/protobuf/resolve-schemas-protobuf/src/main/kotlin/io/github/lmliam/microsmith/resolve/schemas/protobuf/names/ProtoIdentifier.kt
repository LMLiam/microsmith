package io.github.lmliam.microsmith.resolve.schemas.protobuf.names

import arrow.core.Either

@JvmInline
value class ProtoIdentifier private constructor(val value: String) {
    override fun toString(): String = value

    companion object {
        fun parse(raw: String): Either<String, ProtoIdentifier> =
            if (PROTO_IDENTIFIER.matches(raw)) {
                Either.Right(ProtoIdentifier(raw))
            } else {
                Either.Left(raw)
            }

        private val PROTO_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
    }
}
