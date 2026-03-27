package io.github.lmliam.microsmith.compile.schemas.core

import io.github.lmliam.microsmith.artifact.schemas.core.SchemasArtifact
import io.github.lmliam.microsmith.compile.core.ArtifactCompiler

/**
 * Domain-root compiler contract for schema artifacts.
 */
interface SchemasArtifactCompiler<A : SchemasArtifact> : ArtifactCompiler<A>
