package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import kotlin.reflect.KClass

@ServiceProvider(ArtifactAssembler::class)
class MsBuildProjectArtifactAssembler : ArtifactAssembler<MsBuildProjectArtifact> {
    override val artifactType: KClass<MsBuildProjectArtifact> = MsBuildProjectArtifact::class

    override fun create(first: ArtifactContribution<MsBuildProjectArtifact>): MsBuildProjectArtifact {
        val contribution = requireContribution(first)
        return MsBuildProjectArtifact(
            id = contribution.artifactId,
            properties = linkedMapOf<MsBuildPropertyName, String>().apply { putAll(contribution.properties) },
            items = contribution.items.toList(),
        )
    }

    override fun merge(
        current: MsBuildProjectArtifact,
        contribution: ArtifactContribution<MsBuildProjectArtifact>,
    ): MsBuildProjectArtifact {
        val next = requireContribution(contribution)
        val mergedProperties = linkedMapOf<MsBuildPropertyName, String>().apply { putAll(current.properties) }
        next.properties.forEach { (name, value) ->
            val existing = mergedProperties[name]
            require(existing == null || existing == value) {
                "Conflicting MSBuild property '${name.value}' for '${current.id.kind}' in solution " +
                    "'${current.id.solutionName}'${current.id.projectName?.let { " project '$it'" }.orEmpty()}."
            }
            mergedProperties[name] = value
        }

        val mergedItems = linkedMapOf<MsBuildItemIdentity, MsBuildItem>()
        current.items.forEach { item ->
            mergedItems[MsBuildItemIdentity(item.itemName, item.include)] = item
        }
        next.items.forEach { item ->
            val key = MsBuildItemIdentity(item.itemName, item.include)
            val existing = mergedItems[key]
            require(existing == null || existing == item) {
                "Conflicting MSBuild item '${item.itemName}:${item.include}' for " +
                    "'${current.id.kind}' in solution '${current.id.solutionName}'" +
                    current.id.projectName?.let { " project '$it'" }.orEmpty() +
                    "."
            }
            mergedItems[key] = item
        }

        return current.copy(
            properties = mergedProperties,
            items = mergedItems.values.toList(),
        )
    }

    private fun requireContribution(
        contribution: ArtifactContribution<MsBuildProjectArtifact>,
    ): MsBuildProjectContribution {
        require(contribution is MsBuildProjectContribution) {
            "Unsupported MSBuild project contribution type: ${contribution::class}"
        }
        return contribution
    }
}

private data class MsBuildItemIdentity(
    val itemName: String,
    val include: String,
)
