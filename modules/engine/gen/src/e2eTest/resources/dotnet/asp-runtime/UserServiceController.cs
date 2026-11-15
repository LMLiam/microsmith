namespace UserService.Api.Controllers;

using System.Globalization;
using System.Threading;
using System.Threading.Tasks;
using UserService.Api.Generated.Contracts;
using UserService.Api.Generated.Controllers;

public sealed class UserServiceController : UserServiceApiControllerBase
{
    protected override Task<GetUserResult> OnGetUserAsync(
        GetUserPath path,
        GetUserQuery query,
        GetUserHeaders headers,
        CancellationToken cancellationToken)
    {
        if (path.Id == "missing")
        {
            return Task.FromResult<GetUserResult>(
                new GetUserNotFound(
                    new Problem
                    {
                        Detail = "missing-user",
                    }));
        }

        return Task.FromResult<GetUserResult>(
            new GetUserOk(
                new User
                {
                    Id = path.Id,
                    Email = query.IncludeDetails ? "details@example.com" : "basic@example.com",
                },
                Etag: $"etag-{headers.XCorrelationId ?? path.Id}"));
    }

    protected override Task<CreateUserResult> OnCreateUserAsync(
        CreateUserBody body,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(body.Email))
        {
            return Task.FromResult<CreateUserResult>(
                new CreateUserBadRequest(
                    new Problem
                    {
                        Detail = "email-required",
                    }));
        }

        return Task.FromResult<CreateUserResult>(
            new CreateUserCreated(
                new User
                {
                    Id = "generated-user",
                    Email = body.Email,
                },
                Location: "/users/generated-user"));
    }

    protected override Task<GetReportResult> OnGetReportAsync(
        GetReportPath path,
        GetReportQuery query,
        CancellationToken cancellationToken)
    {
        return Task.FromResult<GetReportResult>(
            new GetReportOk(
                new Report
                {
                    Id = path.ReportId.ToString(),
                    Title = $"{query.Days}:{query.Since:yyyy-MM-dd}:{query.Threshold.ToString(CultureInfo.InvariantCulture)}:{query.Window?.ToString() ?? "none"}",
                }));
    }
}
