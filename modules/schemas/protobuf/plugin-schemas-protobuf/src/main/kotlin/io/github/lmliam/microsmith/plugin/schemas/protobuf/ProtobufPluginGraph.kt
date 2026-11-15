package io.github.lmliam.microsmith.plugin.schemas.protobuf

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.github.lmliam.microsmith.artifact.schemas.protobuf.assembly.ProtoFileMerge
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionIssueDiagnosticMapper
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufPluginScope
import io.github.lmliam.microsmith.resolve.schemas.protobuf.diagnostics.ProtobufResolutionDiagnosticMapper

@DependencyGraph(ProtobufPluginScope::class)
internal interface ProtobufPluginGraph {
    val services: ProtobufPluginServices

    @Provides fun protoFileMerge(): ProtoFileMerge = ProtoFileMerge()

    @Provides @IntoSet fun diagnosticMapper(): ResolutionIssueDiagnosticMapper<*> = ProtobufResolutionDiagnosticMapper()

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(): ProtobufPluginGraph
    }
}
