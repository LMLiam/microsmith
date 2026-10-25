# DSL reference

## DSL usage

### Core DSL

```kotlin
val model = microsmith {
    // schemas { ... }
}

val extension = model.get<YourExtensionType>()
val allExtensions = model.extensions()
val typedExtensions = model.extensions().filterIsInstance<YourExtensionType>()
```

### Protobuf schema DSL

```kotlin
microsmith {
    schemas {
        protobuf {
            message("UserCreated") {
                int32("id") { index(1) }
                string("email") { index(2) }
            }

            enum("UserStatus") {
                +"ACTIVE"
                +"DISABLED"
            }

            service("UserService") {
                "GetUser" {
                    "GetUserRequest" to "GetUserResponse"
                }
                "WatchUsers" {
                    "WatchUsersRequest" to stream("WatchUsersResponse")
                }
            }
        }
    }
}
```

### ASP.NET service generation DSL

```kotlin
microsmith {
    services {
        dotnet {
            target(NET8)
            solutions {
                "Platform" {}
            }
        }

        "UserService" {
            dotnet {
                solution("Platform")
                project("UserService.Api")
                models {
                    "User" {
                        string("id")
                        string("email")
                    }
                    "Problem" {
                        string("detail")
                    }
                }
                asp {
                    rest {
                        "/users" {
                            get("ListUsers") {
                                query("ListUsersQuery") {
                                    string("cursor") {
                                        optional()
                                    }
                                }
                                responses {
                                    ok("User")
                                }
                            }

                            "/{id}" {
                                get("GetUser") {
                                    path("GetUserPath") {
                                        string("id")
                                    }
                                    headers("GetUserHeaders") {
                                        header("X-Correlation-Id")
                                    }
                                    responses {
                                        ok("User") {
                                            headers {
                                                header("ETag")
                                            }
                                        }
                                        notFound("Problem")
                                    }
                                }
                            }

                            post("CreateUser") {
                                body("CreateUserBody") {
                                    string("email")
                                }
                                responses {
                                    created("CreateUserResponse") {
                                        model {
                                            string("id")
                                        }
                                        headers {
                                            header("Location")
                                        }
                                    }
                                    badRequest("Problem")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

REST DSL contract notes:

- `asp { ... }` is the canonical surface; `aspNet { ... }` is supported as an alias when the longer spelling reads better in a script
- route groups can be nested, verb helpers are lower-case, and every endpoint requires an explicit operation name
- request bindings are modeled explicitly through `path(...)`, `query(...)`, `headers(...)`, and `body(...)`
- inline body and response models stay endpoint-local in the normalized model; shared service models must be declared under `models { ... }`
- route placeholders are validated against `path(...)` bindings, response/header declarations are normalized, and invalid REST declarations fail during resolution before any ASP.NET source files are emitted

The generated ASP.NET project currently emits this canonical layout under the run output root:

```text
dotnet/
  Platform/
    UserService.Api/
      UserService.Api.csproj
      Program.cs
      appsettings.json
      Generated/
        Hosting/
          MicrosmithHostingExtensions.cs
        Contracts/
          ServiceModels.cs
          RequestModels.cs
          ResponseModels.cs
        Controllers/
          MicrosmithControllerBase.cs
          UserServiceApiControllerBase.cs
      Properties/
        launchSettings.json
      .microsmith/
        origins.json
```

Canonical generation policy:

- `Program.cs` uses top-level hosting and delegates ASP.NET registration through generated hosting extensions
- generated files under `Generated/` provide the contract records, abstract controller base, and response/result mapping surface derived from the normalized REST model
- handwritten service behavior belongs outside `Generated/`, typically in a user-authored controller that derives from the generated base type
- generator-owned scaffold files such as `Program.cs`, `appsettings.json`, `Properties/launchSettings.json`, and everything under `Generated/` are overwritten in place on rerun
- handwritten files outside those generator-owned paths are not overwritten by Microsmith
- `.microsmith/origins.json` records the structural Microsmith origins associated with each generated file

### Script defaults

Inside `.microsmith.kts` scripts:

- default imports include `microsmith {}`, `services {}`, `.NET`/ASP.NET entrypoints, package helpers, `schemas {}`, and `protobuf {}`
- a script can return a `MicrosmithModel`
- a script can also call `emit(model)` or `generate(model)`

[Back to the README](../README.md)
