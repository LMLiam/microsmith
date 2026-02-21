package me.liam.microsmith.gen.schemas.protobuf.emission

/**
 * Signals an invalid in-memory protobuf model shape while rendering or validating.
 *
 * This is reserved for defensive checks that should never be hit through the DSL.
 */
internal class ProtobufEmissionInvariantException(message: String) : IllegalStateException(message)
