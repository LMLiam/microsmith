package io.github.lmliam.microsmith.compile.services

import io.github.lmliam.microsmith.artifact.services.ServicesArtifact
import io.github.lmliam.microsmith.compile.ArtifactCompiler

/** Domain-root compiler contract for service artifacts. */
interface ServicesArtifactCompiler<A : ServicesArtifact> : ArtifactCompiler<A>
