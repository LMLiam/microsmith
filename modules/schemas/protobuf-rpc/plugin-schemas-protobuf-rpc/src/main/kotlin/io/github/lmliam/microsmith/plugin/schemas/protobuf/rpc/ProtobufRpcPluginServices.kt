package io.github.lmliam.microsmith.plugin.schemas.protobuf.rpc

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.plugin.PluginServices
import io.github.lmliam.microsmith.resolve.DomainResolver
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionIssueDiagnosticMapper

@Inject
internal class ProtobufRpcPluginServices(
    override val domainResolvers: Set<DomainResolver<*, *>>,
    override val resolutionIssueDiagnosticMappers: Set<ResolutionIssueDiagnosticMapper<*>>,
    override val artifactContributors: Set<ArtifactContributor<*>>,
    override val artifactAssemblers: Set<ArtifactAssembler<*>>,
    override val artifactCompilers: Set<ArtifactCompiler<*>>,
) : PluginServices
