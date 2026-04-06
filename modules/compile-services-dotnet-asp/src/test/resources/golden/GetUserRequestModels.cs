using System;

namespace UserService.Api.Generated.Contracts;

public sealed record GetUserPath
{
    public string Id { get; set; } = null!;
}

public sealed record GetUserHeaders
{
    public string? XCorrelationId { get; set; }
}
