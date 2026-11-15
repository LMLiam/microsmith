package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.schemas.protobuf.assembly.ProtoFileMerge
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ProtobufRpcPluginScope

@Inject
@ContributesIntoSet(ProtobufRpcPluginScope::class, binding = binding<ArtifactAssembler<*>>())
internal class ProtobufRpcServiceArtifactAssembler(private val merge: ProtoFileMerge) :
    ArtifactAssembler<ProtobufRpcServiceArtifact> {
    override val artifactType = ProtobufRpcServiceArtifact::class

    override fun create(contribution: ArtifactContribution<ProtobufRpcServiceArtifact>): ProtobufRpcServiceArtifact {
        val rpcContribution = requireContribution(contribution)
        return ProtobufRpcServiceArtifact(
            id = rpcContribution.artifactId,
            imports = rpcContribution.imports,
            operations = rpcContribution.operations,
        )
    }

    override fun merge(
        current: ProtobufRpcServiceArtifact,
        contribution: ArtifactContribution<ProtobufRpcServiceArtifact>,
    ): ProtobufRpcServiceArtifact {
        val next = requireContribution(contribution)
        val owner = current.id.protoFile.fullyQualifiedName

        return current.copy(
            imports = merge.imports(current.imports, next.imports),
            operations = merge.members(current.operations, next.operations, "RPC", owner) { it.name },
        )
    }

    private fun requireContribution(
        contribution: ArtifactContribution<ProtobufRpcServiceArtifact>,
    ): ProtobufRpcServiceContribution {
        require(contribution is ProtobufRpcServiceContribution) {
            "Unsupported protobuf RPC contribution type: ${contribution::class}"
        }
        return contribution
    }
}
