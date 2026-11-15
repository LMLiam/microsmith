namespace Platform.Api.Generated;

public abstract class UsersControllerBase
{
    [HttpGet("/users/{id}", Name = "GetUser")]
    public string? GetUser([FromRoute] string id)
    {
        if (Request.Headers.TryGetValue("X-Correlation-Id", out var values))
        {
            return values.ToString();
        }

        return throw new InvalidOperationException("Missing header");
    }
}
