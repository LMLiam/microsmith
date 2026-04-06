using Microsoft.AspNetCore.Mvc;
using System;
using System.Threading;
using System.Threading.Tasks;
using UserService.Api.Generated.Contracts;

namespace UserService.Api.Generated.Controllers;

[ApiController]
public abstract class UserServiceApiControllerBase : ControllerBase
{
    [HttpGet("/users/{id}", Name = "GetUser")]
    public async Task<ActionResult<GetUserResult>> GetUser(
        [FromRoute] GetUserPath path,
        CancellationToken cancellationToken
    )
    {
        var headers = new GetUserHeaders
        {
            XCorrelationId = ReadHeader("X-Correlation-Id")
        };

        var result = await OnGetUserAsync(path, headers, cancellationToken);
        return MapGetUserResult(result);
    }

    protected abstract Task<GetUserResult> OnGetUserAsync(
        GetUserPath path,
        GetUserHeaders headers,
        CancellationToken cancellationToken
    );

    private ActionResult<GetUserResult> MapGetUserResult(GetUserResult result)
    {
        return result switch
        {
            GetUserOk response => Respond(response.Body, 200),
            _ => throw new InvalidOperationException(
                "Unsupported GetUser result type '${result.GetType().FullName}'."
            )
        };
    }

    private ObjectResult Respond(
        object body,
        int statusCode,
        params (string Name, string? Value)[] headers
    )
    {
        foreach (var (name, value) in headers)
        {
            if (value is not null)
            {
                Response.Headers[name] = value;
            }
        }

        return new ObjectResult(body)
        {
            StatusCode = statusCode,
        };
    }

    private string? ReadHeader(string headerName)
    {
        return Request.Headers.TryGetValue(headerName, out var values)
            ? values.ToString()
            : null;
    }
}
