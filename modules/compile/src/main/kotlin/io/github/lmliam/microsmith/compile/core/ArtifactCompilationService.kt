package io.github.lmliam.microsmith.compile.core

import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactAssembly
import io.github.lmliam.microsmith.artifact.core.ArtifactAssemblyService

class ArtifactCompilationService internal constructor(
    private val compilerRegistry: ArtifactCompilerRegistry = ArtifactCompilerRegistry(),
    private val assemblyService: ArtifactAssemblyService = ArtifactAssemblyService(),
) {
    constructor() : this(ArtifactCompilerRegistry(), ArtifactAssemblyService())

    constructor(
        compilers: List<ArtifactCompiler<*>>,
        assemblyService: ArtifactAssemblyService = ArtifactAssemblyService(),
    ) : this(ArtifactCompilerRegistry(compilers), assemblyService)

    fun compile(assembly: ArtifactAssembly): ArtifactAssembly = compileUntilStable(assembly, linkedSetOf())

    private tailrec fun compileUntilStable(
        current: ArtifactAssembly,
        seenSignatures: MutableSet<String>,
    ): ArtifactAssembly {
        val signature = current.signature()
        require(seenSignatures.add(signature)) {
            "Artifact compilation cycle detected for assembly: $signature"
        }

        val next = compileSinglePass(current) ?: return current
        return compileUntilStable(next, seenSignatures)
    }

    private fun compileSinglePass(current: ArtifactAssembly): ArtifactAssembly? {
        val passthroughArtifacts = mutableListOf<Artifact>()
        val compiledContributions =
            mutableListOf<io.github.lmliam.microsmith.artifact.core.ArtifactContribution<out Artifact>>()
        var compiledAny = false

        current.artifacts().forEach { artifact ->
            val compiler = compilerRegistry.resolveOrNull(artifact)
            if (compiler == null) {
                passthroughArtifacts += artifact
                return@forEach
            }

            val contributions = compiler.compileUnchecked(artifact)
            require(contributions.none { it.artifactId.artifactType == artifact.id.artifactType }) {
                val compilerName = compiler::class.qualifiedName ?: compiler::class.toString()
                val artifactTypeName = artifact.id.artifactType.toString()
                "Artifact compiler $compilerName compiled $artifactTypeName into the same artifact type, " +
                    "which would create an immediate compilation cycle."
            }
            compiledContributions += contributions
            compiledAny = true
        }

        if (!compiledAny) {
            return null
        }

        return assemblyService.assembleRetaining(passthroughArtifacts, compiledContributions)
    }

    private fun ArtifactAssembly.signature(): String = artifacts()
        .map { artifact ->
            val typeName = artifact.id.artifactType.qualifiedName ?: artifact.id.artifactType.toString()
            "$typeName:${artifact.id}"
        }
        .sorted()
        .joinToString("|")
}

@Suppress("UNCHECKED_CAST")
private fun ArtifactCompiler<Artifact>.compileUnchecked(
    artifact: Artifact,
): List<io.github.lmliam.microsmith.artifact.core.ArtifactContribution<out Artifact>> =
    (this as ArtifactCompiler<Artifact>).compile(artifact)
