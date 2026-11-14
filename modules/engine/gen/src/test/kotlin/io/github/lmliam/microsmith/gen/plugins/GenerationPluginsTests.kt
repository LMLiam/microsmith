package io.github.lmliam.microsmith.gen.plugins

import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactId
import io.github.lmliam.microsmith.artifact.ArtifactRenderer
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.files.GeneratedFile
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.gen.errors.PluginConflictException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain

private data class FirstArtifact(override val id: FirstArtifactId) : Artifact

private data class FirstArtifactId(val name: String) : ArtifactId<FirstArtifact> {
    override val artifactType = FirstArtifact::class
}

private data class SecondArtifact(override val id: SecondArtifactId) : Artifact

private data class SecondArtifactId(val name: String) : ArtifactId<SecondArtifact> {
    override val artifactType = SecondArtifact::class
}

private class FirstArtifactCompiler : ArtifactCompiler<FirstArtifact> {
    override val artifactType = FirstArtifact::class

    override fun compile(artifact: FirstArtifact): List<ArtifactContribution<Artifact>> = emptyList()
}

private class SecondArtifactCompiler : ArtifactCompiler<SecondArtifact> {
    override val artifactType = SecondArtifact::class

    override fun compile(artifact: SecondArtifact): List<ArtifactContribution<Artifact>> = emptyList()
}

private class FirstArtifactAssembler : ArtifactAssembler<FirstArtifact> {
    override val artifactType = FirstArtifact::class

    override fun create(contribution: ArtifactContribution<FirstArtifact>): FirstArtifact = error("unused")

    override fun merge(current: FirstArtifact, contribution: ArtifactContribution<FirstArtifact>): FirstArtifact =
        error("unused")
}

private class FirstArtifactRenderer : ArtifactRenderer<FirstArtifact> {
    override val artifactType = FirstArtifact::class

    override fun render(artifact: FirstArtifact): GeneratedFile =
        GeneratedFile(RelativePath.of("first.txt"), byteArrayOf())
}

class GenerationPluginsTests :
    StringSpec(
        {
            "two plugins with a compiler for one artifact type are reported with both plugin ids" {
                val failure =
                    shouldThrow<PluginConflictException> {
                        GenerationPlugins.requireOneServicePerArtifactType(
                            mapOf(
                                "plugin 'acme.one'" to
                                    MicrosmithPluginCatalog(artifactCompilers = listOf(FirstArtifactCompiler())),
                                "plugin 'acme.two'" to
                                    MicrosmithPluginCatalog(artifactCompilers = listOf(FirstArtifactCompiler())),
                            ),
                        )
                    }

                failure.message shouldContain "compiler"
                failure.message shouldContain "plugin 'acme.one'"
                failure.message shouldContain "plugin 'acme.two'"
                failure.message shouldContain FirstArtifact::class.qualifiedName!!
            }

            "two plugins with an assembler for one artifact type are rejected" {
                val failure =
                    shouldThrow<PluginConflictException> {
                        GenerationPlugins.requireOneServicePerArtifactType(
                            mapOf(
                                "plugin 'acme.one'" to
                                    MicrosmithPluginCatalog(artifactAssemblers = listOf(FirstArtifactAssembler())),
                                "plugin 'acme.two'" to
                                    MicrosmithPluginCatalog(artifactAssemblers = listOf(FirstArtifactAssembler())),
                            ),
                        )
                    }

                failure.message shouldContain "assembler"
            }

            "a plugin renderer that claims a type already rendered by the host names the host and the plugin" {
                val failure =
                    shouldThrow<PluginConflictException> {
                        GenerationPlugins.requireOneServicePerArtifactType(
                            mapOf(
                                "the host" to
                                    MicrosmithPluginCatalog(artifactRenderers = listOf(FirstArtifactRenderer())),
                                "plugin 'acme.one'" to
                                    MicrosmithPluginCatalog(artifactRenderers = listOf(FirstArtifactRenderer())),
                            ),
                        )
                    }

                failure.message shouldContain "renderer"
                failure.message shouldContain "the host"
                failure.message shouldContain "plugin 'acme.one'"
            }

            "compilers for different artifact types from different plugins are accepted" {
                GenerationPlugins.requireOneServicePerArtifactType(
                    mapOf(
                        "plugin 'acme.one'" to
                            MicrosmithPluginCatalog(artifactCompilers = listOf(FirstArtifactCompiler())),
                        "plugin 'acme.two'" to
                            MicrosmithPluginCatalog(artifactCompilers = listOf(SecondArtifactCompiler())),
                    ),
                )
            }
        },
    )
