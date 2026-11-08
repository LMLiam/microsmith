package io.github.lmliam.microsmith.compile

import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembly
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssemblyService

class ArtifactCompilationService
internal constructor(
    private val compilerRegistry: ArtifactCompilerRegistry,
    private val assemblyService: ArtifactAssemblyService,
) {
    constructor(
        compilers: List<ArtifactCompiler<*>>,
        assemblyService: ArtifactAssemblyService,
    ) : this(ArtifactCompilerRegistry(compilers), assemblyService)

    fun compile(assembly: ArtifactAssembly): ArtifactAssembly =
        compileUntilStable(
            current = assembly,
            seenSignatures = mutableSetOf(),
        )

    private tailrec fun compileUntilStable(
        current: ArtifactAssembly,
        seenSignatures: MutableSet<String>,
    ): ArtifactAssembly {
        val signature = current.signature()

        require(seenSignatures.add(signature)) { "Artifact compilation cycle detected for assembly: '$signature'." }

        val next = compileSinglePass(current) ?: return current

        return compileUntilStable(next, seenSignatures)
    }

    private fun compileSinglePass(current: ArtifactAssembly): ArtifactAssembly? {
        val passthroughArtifacts = mutableListOf<Artifact>()
        val compiledContributions = mutableListOf<ArtifactContribution<Artifact>>()
        var compiledAny = false

        for (artifact in current.artifacts()) {
            val compiler = compilerRegistry.resolveOrNull(artifact)
            if (compiler == null) {
                passthroughArtifacts += artifact
                continue
            }

            val contributions = compiler.compile(artifact)

            require(contributions.none { it.artifactId.artifactType == artifact.id.artifactType }) {
                "'${compiler::class.displayName}' compiled '${artifact.id.artifactType.displayName}' " +
                    "into the same artifact type, creating an immediate compilation cycle."
            }

            compiledContributions += contributions
            compiledAny = true
        }

        if (!compiledAny) return null

        return assemblyService.assembleRetaining(
            retainedArtifacts = passthroughArtifacts,
            contributions = compiledContributions,
        )
    }

    private fun ArtifactAssembly.signature(): String =
        artifacts().map { "${it.id.artifactType.displayName}:${it.id}" }.sorted().joinToString("|")
}
