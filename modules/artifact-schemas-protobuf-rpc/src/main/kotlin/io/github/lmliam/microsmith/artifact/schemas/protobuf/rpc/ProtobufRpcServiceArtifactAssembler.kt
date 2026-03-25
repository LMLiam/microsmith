package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import kotlin.reflect.KClass

@ServiceProvider(ArtifactAssembler::class)
class ProtobufRpcServiceArtifactAssembler : ArtifactAssembler<ProtobufRpcServiceArtifact> {
    override val artifactType: KClass<ProtobufRpcServiceArtifact> = ProtobufRpcServiceArtifact::class

    override fun create(first: ArtifactContribution<ProtobufRpcServiceArtifact>): ProtobufRpcServiceArtifact {
        val contribution = requireContribution(first)
        return ProtobufRpcServiceArtifact(
            id = contribution.artifactId,
            imports = contribution.imports.distinct().sorted(),
            operations = contribution.operations,
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
        contribution: ArtifactContribution<ProtobufRpcServiceArtifact>,
    ): ProtobufRpcServiceContribution {
        require(contribution is ProtobufRpcServiceContribution) {
            "Unsupported protobuf RPC contribution type: ${contribution::class}"
        }
        return contribution
    }
}
