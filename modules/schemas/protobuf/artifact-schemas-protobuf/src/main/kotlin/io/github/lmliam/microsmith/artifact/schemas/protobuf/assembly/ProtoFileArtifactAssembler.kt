package io.github.lmliam.microsmith.artifact.schemas.protobuf.assembly

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.schemas.protobuf.contribution.ProtoFileContribution
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifact
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufPluginScope

@Inject
@ContributesIntoSet(ProtobufPluginScope::class, binding = binding<ArtifactAssembler<*>>())
internal class ProtoFileArtifactAssembler(private val merge: ProtoFileMerge) : ArtifactAssembler<ProtoFileArtifact> {
    override val artifactType = ProtoFileArtifact::class

    override fun create(contribution: ArtifactContribution<ProtoFileArtifact>): ProtoFileArtifact {
        val protoContribution = requireContribution(contribution)
        return ProtoFileArtifact(
            id = protoContribution.artifactId,
            imports = protoContribution.imports,
            declarations = protoContribution.declarations,
        )
    }

    override fun merge(
        current: ProtoFileArtifact,
        contribution: ArtifactContribution<ProtoFileArtifact>,
    ): ProtoFileArtifact {
        val next = requireContribution(contribution)
        val owner = current.id.fullyQualifiedName

        return current.copy(
            imports = merge.imports(current.imports, next.imports),
            declarations =
                merge.members(current.declarations, next.declarations, ProtoFileMemberKind.DECLARATION, owner) {
                    it.name
                },
        )
    }

    private fun requireContribution(contribution: ArtifactContribution<ProtoFileArtifact>): ProtoFileContribution {
        require(contribution is ProtoFileContribution) {
            "Unsupported protobuf artifact contribution type: " + "${contribution::class}"
        }
        return contribution
    }
}
