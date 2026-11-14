package io.github.lmliam.microsmith.artifact.services.dotnet.asp.request

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspDefaultValue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestField
import tech.mappie.api.ObjectMappie

internal object DotnetAspRequestFieldArtifactMapper :
    ObjectMappie<ResolvedDotnetAspRequestField, DotnetAspRequestFieldArtifact>() {
    override fun map(from: ResolvedDotnetAspRequestField): DotnetAspRequestFieldArtifact = mapping {
        to::defaultValue fromProperty from::defaultValue transform { it?.unwrapDotnetAspDefaultValue() }
    }
}

private fun DotnetAspDefaultValue.unwrapDotnetAspDefaultValue(): Any =
    when (this) {
        is DotnetAspDefaultValue.StringValue -> value
        is DotnetAspDefaultValue.CharValue -> value
        is DotnetAspDefaultValue.NumericValue -> value
        is DotnetAspDefaultValue.BooleanValue -> value
        is DotnetAspDefaultValue.UuidValue -> value
        is DotnetAspDefaultValue.LocalDateValue -> value
        is DotnetAspDefaultValue.LocalTimeValue -> value
        is DotnetAspDefaultValue.LocalDateTimeValue -> value
        is DotnetAspDefaultValue.InstantValue -> value
        is DotnetAspDefaultValue.OffsetDateTimeValue -> value
        is DotnetAspDefaultValue.DurationValue -> value
    }
