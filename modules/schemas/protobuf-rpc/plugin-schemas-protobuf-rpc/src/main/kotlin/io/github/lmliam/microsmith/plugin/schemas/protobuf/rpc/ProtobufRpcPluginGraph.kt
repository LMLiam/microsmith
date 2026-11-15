package io.github.lmliam.microsmith.plugin.schemas.protobuf.rpc

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import io.github.lmliam.microsmith.artifact.schemas.protobuf.assembly.ProtoFileMerge
import io.github.lmliam.microsmith.resolve.schemas.protobuf.diagnostics.ProtobufResolutionDiagnosticMapper
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ProtobufRpcPluginScope

@DependencyGraph(ProtobufRpcPluginScope::class)
internal interface ProtobufRpcPluginGraph {
    val services: ProtobufRpcPluginServices

    @Provides fun protoFileMerge(): ProtoFileMerge = ProtoFileMerge()

    @Provides
    fun protobufResolutionDiagnosticMapper(): ProtobufResolutionDiagnosticMapper = ProtobufResolutionDiagnosticMapper()

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(): ProtobufRpcPluginGraph
    }
}
