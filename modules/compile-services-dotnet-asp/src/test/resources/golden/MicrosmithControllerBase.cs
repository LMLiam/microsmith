using Microsoft.AspNetCore.Mvc;

namespace UserService.Api.Generated.Controllers;

public abstract class MicrosmithControllerBase : ControllerBase
{
    protected ObjectResult Respond(object body, int statusCode, params (string Name, string? Value)[] headers)
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
            StatusCode = statusCode
        };
    }

    protected string? ReadHeader(string headerName)
    {
        return Request.Headers.TryGetValue(headerName, out var values)
            ? values.ToString()
            : null;
    }
}
