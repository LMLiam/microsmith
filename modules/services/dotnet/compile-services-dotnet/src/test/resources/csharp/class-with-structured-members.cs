using Microsoft.AspNetCore.Mvc;
using System.Threading.Tasks;

namespace Platform.Api.Generated;

[ApiController]
public abstract class UsersControllerBase : ControllerBase
{
    public string Name { get; set; } = "Users";

    public abstract Task<ActionResult<User>> GetUserAsync(CancellationToken cancellationToken);
}
