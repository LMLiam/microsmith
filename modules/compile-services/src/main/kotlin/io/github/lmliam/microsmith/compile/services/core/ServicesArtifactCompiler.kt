package io.github.lmliam.microsmith.compile.services.core

import io.github.lmliam.microsmith.artifact.services.core.ServicesArtifact
import io.github.lmliam.microsmith.compile.core.ArtifactCompiler

/**
 * Domain-root compiler contract for service artifacts.
 */
interface ServicesArtifactCompiler<A : ServicesArtifact> : ArtifactCompiler<A>
