package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.service

internal fun mergeDotnetAspRest(existing: DotnetAspRest, incoming: DotnetAspRest) = DotnetAspRest(
    groups = existing.groups + incoming.groups,
    endpoints = existing.endpoints + incoming.endpoints,
)
