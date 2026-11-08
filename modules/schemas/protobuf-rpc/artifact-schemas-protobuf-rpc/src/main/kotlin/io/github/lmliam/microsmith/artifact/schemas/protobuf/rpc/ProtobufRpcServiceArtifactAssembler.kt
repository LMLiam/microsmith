package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler

@ServiceProvider(ArtifactAssembler::class)
class ProtobufRpcServiceArtifactAssembler : ArtifactAssembler<ProtobufRpcServiceArtifact> {
    override val artifactType = ProtobufRpcServiceArtifact::class

    override fun create(contribution: ArtifactContribution<ProtobufRpcServiceArtifact>): ProtobufRpcServiceArtifact {
        val rpcContribution = requireContribution(contribution)
        return ProtobufRpcServiceArtifact(
            id = rpcContribution.artifactId,
            imports = rpcContribution.imports.distinct().sorted(),
            operations = rpcContribution.operations,
        )
    }

    override fun merge(
        current: ProtobufRpcServiceArtifact,
        contribution: ArtifactContribution<ProtobufRpcServiceArtifact>,
    ): ProtobufRpcServiceArtifact {
        val next = requireContribution(contribution)
        val mergedOperations = LinkedHashMap(current.operations.associateBy(ProtobufRpcOperation::name))
        next.operations.forEach { operation ->
            val existing = mergedOperations[operation.name]
            require(existing == null || existing == operation) {
                "Conflicting protobuf RPC '${operation.name}' for '${current.id.fullyQualifiedName}'."
            }
            mergedOperations.putIfAbsent(operation.name, operation)
        }
        return current.copy(
            imports = (current.imports + next.imports).distinct().sorted(),
            operations = mergedOperations.values.toList(),
        )
    }

    private fun requireContribution(
        contribution: ArtifactContribution<ProtobufRpcServiceArtifact>
    ): ProtobufRpcServiceContribution {
        require(contribution is ProtobufRpcServiceContribution) {
            "Unsupported protobuf RPC contribution type: ${contribution::class}"
        }
        return contribution
    }
}
