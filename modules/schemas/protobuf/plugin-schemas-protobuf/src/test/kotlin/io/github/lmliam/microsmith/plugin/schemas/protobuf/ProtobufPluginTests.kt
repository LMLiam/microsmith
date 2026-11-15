package io.github.lmliam.microsmith.plugin.schemas.protobuf

import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifact
import io.github.lmliam.microsmith.plugin.Console
import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import io.github.lmliam.microsmith.plugin.Environment
import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.nio.file.FileSystems
import kotlin.time.Clock

class ProtobufPluginTests :
    StringSpec(
        {
            val recorded = mutableListOf<String>()
            val host =
                object : HostServices {
                    override val fileSystem = FileSystems.getDefault()
                    override val clock = Clock.System
                    override val diagnostics =
                        object : DiagnosticSink {
                            override fun report(diagnostic: Diagnostic) {
                                recorded += diagnostic.toString()
                            }
                        }
                    override val console =
                        object : Console {
                            override fun out(line: String) {
                                recorded += line
                            }

                            override fun err(line: String) {
                                recorded += line
                            }
                        }
                    override val environment =
                        object : Environment {
                            override fun variable(name: String): String? = null

                            override fun property(name: String): String? = null
                        }
                }

            "has the reverse-DNS protobuf plugin id" {
                ProtobufPlugin().id shouldBe "io.github.lmliam.microsmith.protobuf"
            }

            "contributes the protobuf resolver, contributor, assembler and compiler" {
                ProtobufPlugin().services(host).use { services ->
                    services.domainResolvers.map { it.resolvedType } shouldContainExactly
                        listOf(ResolvedProtobufSchemaModel::class)
                    services.artifactContributors.map { it.resolvedType } shouldContainExactly
                        listOf(ResolvedProtobufSchemaModel::class)
                    services.artifactAssemblers.map { it.artifactType } shouldContainExactly
                        listOf(ProtoFileArtifact::class)
                    services.artifactCompilers.map { it.artifactType } shouldContainExactly
                        listOf(ProtoFileArtifact::class)
                }
            }

            "contributes the protobuf resolution issue diagnostic mapper" {
                ProtobufPlugin().services(host).use { services ->
                    services.resolutionIssueDiagnosticMappers.map { it.issueType.simpleName } shouldContainExactly
                        listOf("ProtobufResolutionIssue")
                }
            }
        },
    )
