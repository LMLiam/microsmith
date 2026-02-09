package me.liam.microsmith.gen.schemas.protobuf.emission

internal fun invariantViolation(message: String): Nothing = throw ProtobufEmissionInvariantException(message)

internal fun invalidTopLevelOneofField(name: String): Nothing =
    invariantViolation("Oneof field '$name' cannot be a top-level message field.")
