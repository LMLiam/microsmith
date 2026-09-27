package io.github.lmliam.microsmith.compile

import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembly
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssemblyService
import kotlin.collections.linkedSetOf

class ArtifactCompilationService(
    compilers: List<ArtifactCompiler<*>>,
    private val assemblyService: ArtifactAssemblyService,
) {
    private val compilerRegistry = ArtifactCompilerRegistry(compilers)

    fun compile(assembly: ArtifactAssembly): ArtifactAssembly = compileUntilStable(
        current = assembly,
        seenSignatures = linkedSetOf(),
    )

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
            mutableListOf<ArtifactContribution<out Artifact>>()

        var compiledAny = false

        current.artifacts().forEach { artifact ->
            val compiler = compilerRegistry.resolveOrNull(artifact)

            if (compiler == null) {
                passthroughArtifacts += artifact
                return@forEach
            }

            val contributions = compiler.compile(artifact)

            require(
                contributions.none {
                    it.artifactId.artifactType == artifact.id.artifactType
                },
            ) {
                val compilerName = compiler::class.qualifiedName ?: compiler::class.toString()

                val artifactTypeName = artifact.id.artifactType.toString()

                "Artifact compiler $compilerName compiled $artifactTypeName into the same artifact type, " +
                    "which would create an immediate compilation cycle"
            }

            compiledContributions += contributions
            compiledAny = true
        }

        if (!compiledAny) {
            return null
        }

        return assemblyService.assembleRetaining(
            retainedArtifacts = passthroughArtifacts,
            contributions = compiledContributions,
        )
    }

    private fun ArtifactAssembly.signature(): String = artifacts()
        .map { artifact ->
            val typeName = artifact.id.artifactType.qualifiedName
                ?: artifact.id.artifactType.toString()

            "$typeName:${artifact.id}"
        }
        .sorted()
        .joinToString("|")
}
