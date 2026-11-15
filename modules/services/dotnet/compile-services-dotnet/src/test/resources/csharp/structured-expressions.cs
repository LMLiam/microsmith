namespace Platform.Api.Generated;

public sealed class Responder
{
    public ObjectResult Map(ResultBase result, object body)
    {
        var response = new ObjectResult(body)
        {
            StatusCode = 200
        };

        return result switch
        {
            OkResult ok => Respond(ok),
            _ => response
        };
    }
}
