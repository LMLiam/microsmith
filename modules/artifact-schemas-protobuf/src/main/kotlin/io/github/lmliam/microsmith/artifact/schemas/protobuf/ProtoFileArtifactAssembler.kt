package io.github.lmliam.microsmith.artifact.schemas.protobuf

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution

@ServiceProvider(ArtifactAssembler::class)
class ProtoFileArtifactAssembler : ArtifactAssembler<ProtoFileArtifact> {
    override val artifactType = ProtoFileArtifact::class

    override fun create(first: ArtifactContribution<ProtoFileArtifact>): ProtoFileArtifact {
        val contribution = requireContribution(first)
        return ProtoFileArtifact(
            id = contribution.artifactId,
            packageName = contribution.packageName,
            imports = contribution.imports.distinct().sorted(),
            declarations = contribution.declarations,
            origins = contribution.origins,
        )
    }

    override fun merge(
        current: ProtoFileArtifact,
        contribution: ArtifactContribution<ProtoFileArtifact>,
    ): ProtoFileArtifact {
        val next = requireContribution(contribution)
        require(current.packageName == next.packageName) {
            "Conflicting protobuf package names for '${current.id.fullyQualifiedName}'."
        }

        val mergedDeclarations = LinkedHashMap(current.declarations.associateBy(ProtoDeclaration::name))
        next.declarations.forEach { declaration ->
            val existing = mergedDeclarations[declaration.name]
            require(existing == null || existing == declaration) {
                "Conflicting protobuf declaration '${declaration.name}' for " +
                    "'${current.id.fullyQualifiedName}'."
            }
            mergedDeclarations.putIfAbsent(declaration.name, declaration)
        }

        return current.copy(
            imports = (current.imports + next.imports).distinct().sorted(),
            declarations = mergedDeclarations.values.toList(),
            origins = current.origins + next.origins,
        )
    }

    private fun requireContribution(contribution: ArtifactContribution<ProtoFileArtifact>): ProtoFileContribution {
        require(contribution is ProtoFileContribution) {
            "Unsupported protobuf artifact contribution type: " +
                "${contribution::class}"
        }
        return contribution
    }
}
