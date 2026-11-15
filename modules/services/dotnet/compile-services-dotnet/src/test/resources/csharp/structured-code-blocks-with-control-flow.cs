namespace Platform.Api.Generated;

public sealed class Responder
{
    public string Respond(string[] headers)
    {
        foreach (var header in headers)
        {
            if (header.Length > 0)
            {
                Console.WriteLine(header);
            }
        }

        return "ok";
    }
}
