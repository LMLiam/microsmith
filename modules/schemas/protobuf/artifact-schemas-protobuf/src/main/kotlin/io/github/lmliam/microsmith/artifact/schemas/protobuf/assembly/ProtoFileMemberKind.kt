package io.github.lmliam.microsmith.artifact.schemas.protobuf.assembly

import io.github.lmliam.microsmith.InternalMicrosmithApi

@InternalMicrosmithApi
public enum class ProtoFileMemberKind(public val label: String) {
    DECLARATION("declaration"),
    RPC("RPC"),
}
