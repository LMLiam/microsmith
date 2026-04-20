package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel

data class DotnetAspModelArtifact(
    val typeName: String,
    val locality: DotnetAspModelLocality,
    val model: DotnetModel,
    val origins: Set<String>,
)

enum class DotnetAspModelLocality {
    SHARED,
    INLINE,
}

data class DotnetAspEndpointArtifact(
    val method: String,
    val route: String,
    val operationName: String,
    val bindings: DotnetAspEndpointBindingsArtifact,
    val responses: List<DotnetAspResponseArtifact>,
    val origins: Set<String>,
)

data class DotnetAspEndpointBindingsArtifact(
    val path: DotnetAspRequestBindingArtifact? = null,
    val query: DotnetAspRequestBindingArtifact? = null,
    val headers: DotnetAspHeadersBindingArtifact? = null,
    val body: DotnetAspModelArtifact? = null,
)

data class DotnetAspRequestBindingArtifact(
    val typeName: String,
    val name: String,
    val fields: List<DotnetAspRequestFieldArtifact>,
    val origins: Set<String>,
)

data class DotnetAspRequestFieldArtifact(
    val name: String,
    val type: DotnetFieldType,
    val optional: Boolean,
    val defaultValue: Any?,
)

data class DotnetAspHeadersBindingArtifact(
    val typeName: String,
    val name: String,
    val headers: List<DotnetAspHeaderFieldArtifact>,
    val origins: Set<String>,
)

data class DotnetAspHeaderFieldArtifact(
    val name: String,
    val headerName: String,
)

data class DotnetAspResponseArtifact(
    val statusCode: Int,
    val model: DotnetAspModelArtifact,
    val headers: List<DotnetAspResponseHeaderArtifact>,
    val origins: Set<String>,
)

data class DotnetAspResponseHeaderArtifact(val name: String)
