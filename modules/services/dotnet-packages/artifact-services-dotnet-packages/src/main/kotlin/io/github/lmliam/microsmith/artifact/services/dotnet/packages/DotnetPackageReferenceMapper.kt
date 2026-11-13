package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import io.github.lmliam.microsmith.artifact.services.dotnet.packages.references.DotnetPackageReference
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.references.DotnetPackageReferenceVersion
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageReference
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageReferenceVersion
import tech.mappie.api.ObjectMappie

internal object DotnetPackageReferenceMapper : ObjectMappie<ResolvedDotnetPackageReference, DotnetPackageReference>() {
    override fun map(from: ResolvedDotnetPackageReference): DotnetPackageReference = mapping {
        to::version fromProperty from::version transform { it.toArtifactVersion() }
    }
}

private fun ResolvedDotnetPackageReferenceVersion.toArtifactVersion(): DotnetPackageReferenceVersion =
    when (this) {
        ResolvedDotnetPackageReferenceVersion.Central -> DotnetPackageReferenceVersion.Central
        is ResolvedDotnetPackageReferenceVersion.Explicit -> DotnetPackageReferenceVersion.Explicit(version)
    }
