package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler

@ServiceProvider(ArtifactAssembler::class)
class MsBuildProjectArtifactAssembler : ArtifactAssembler<MsBuildProjectArtifact> {
    override val artifactType = MsBuildProjectArtifact::class

    override fun create(contribution: ArtifactContribution<MsBuildProjectArtifact>): MsBuildProjectArtifact {
        val projectContribution = requireContribution(contribution)
        return MsBuildProjectArtifact(
            id = projectContribution.artifactId,
            projectAttributes = linkedMapOf<String, String>().apply { putAll(projectContribution.projectAttributes) },
            properties = linkedMapOf<String, String>().apply { putAll(projectContribution.properties) },
            items = projectContribution.items.toList(),
        )
    }

    override fun merge(
        current: MsBuildProjectArtifact,
        contribution: ArtifactContribution<MsBuildProjectArtifact>,
    ): MsBuildProjectArtifact {
        val next = requireContribution(contribution)
        val mergedProjectAttributes = linkedMapOf<String, String>().apply { putAll(current.projectAttributes) }
        next.projectAttributes.forEach { (name, value) ->
            val existing = mergedProjectAttributes[name]
            require(existing == null || existing == value) {
                "Conflicting MSBuild project attribute '$name' for ${current.id.description()}."
            }
            mergedProjectAttributes[name] = value
        }

        val mergedProperties = linkedMapOf<String, String>().apply { putAll(current.properties) }
        next.properties.forEach { (name, value) ->
            val existing = mergedProperties[name]
            require(existing == null || existing == value) {
                "Conflicting MSBuild property '$name' for ${current.id.description()}."
            }
            mergedProperties[name] = value
        }

        val mergedItems = linkedMapOf<MsBuildItemIdentity, MsBuildItem>()
        current.items.forEach { item -> mergedItems[MsBuildItemIdentity(item.itemName, item.include)] = item }
        next.items.forEach { item ->
            val key = MsBuildItemIdentity(item.itemName, item.include)
            val existing = mergedItems[key]
            require(existing == null || existing == item) {
                "Conflicting MSBuild item '${item.itemName}:${item.include}' for ${current.id.description()}."
            }
            mergedItems[key] = item
        }

        return current.copy(
            projectAttributes = mergedProjectAttributes,
            properties = mergedProperties,
            items = mergedItems.values.toList(),
        )
    }

    private fun requireContribution(
        contribution: ArtifactContribution<MsBuildProjectArtifact>
    ): MsBuildProjectContribution {
        require(contribution is MsBuildProjectContribution) {
            "Unsupported MSBuild project contribution type: ${contribution::class}"
        }
        return contribution
    }
}

private data class MsBuildItemIdentity(val itemName: String, val include: String)

private fun MsBuildProjectArtifactId.description(): String =
    when (this) {
        is MsBuildProjectArtifactId.DirectoryPackagesProps -> "'DirectoryPackagesProps' in solution '$solutionName'"
        is MsBuildProjectArtifactId.DirectoryBuildProps ->
            "'DirectoryBuildProps' in solution '$solutionName' project '$projectName'"
        is MsBuildProjectArtifactId.Project -> "'Project' in solution '$solutionName' project '$projectName'"
    }
