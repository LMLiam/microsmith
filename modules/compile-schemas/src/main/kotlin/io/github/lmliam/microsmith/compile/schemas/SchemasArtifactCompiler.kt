package io.github.lmliam.microsmith.compile.schemas
import io.github.lmliam.microsmith.artifact.schemas.SchemasArtifact
import io.github.lmliam.microsmith.compile.ArtifactCompiler

/**
 * Domain-root compiler contract for schema artifacts.
 */
interface SchemasArtifactCompiler<A : SchemasArtifact> : ArtifactCompiler<A>
