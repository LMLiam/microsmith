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

Declare messages and enums, then refer to them by name as typed values. A literal `message("Name")` or `enum("Name")` inside `protobuf { }` gives the script a value called `Name`, of type `MessageRef` or `EnumRef`.

```kotlin
microsmith {
    schemas {
        protobuf {
            service("UserService") {
                "GetUser" {
                    request(GetUserRequest)
                    response(GetUserResponse)
                }
                "WatchUsers" {
                    request(WatchUsersRequest)
                    response(WatchUsersResponse) { stream() }
                }
            }

            message("GetUserRequest") {
                int32("id") { index(1) }
            }

            message("GetUserResponse") {
                string("email") { index(1) }
                ref("status", UserStatus) { index(2) }
            }

            message("WatchUsersRequest")
            message("WatchUsersResponse")

            enum("UserStatus") {
                +"ACTIVE"
                +"DISABLED"
            }
        }
    }
}
```

An enum gets a generated `USER_STATUS_UNSPECIFIED = 0` value unless one of its values claims index 0, as in `value("OK") { index(0) }`. Values without an index are numbered from 1.

Typed references behave like this:

- symbols exist in `.microsmith.kts` scripts. In ordinary Kotlin code, use string references or `MessageRef("...")`
- a declaration can be used before it appears in the script
- a declaration inside a namespace or `version(...)` block gets its qualified name automatically
- a `MessageRef` is required where a message is expected. Passing an `EnumRef` to `request(...)` fails script compilation
- `request(...)`, `response(...)`, `stream(...)`, and the `to` shorthand accept a `MessageRef`. `ref(...)` accepts a `MessageRef` or an `EnumRef`
- each declaration gets a qualified symbol: its namespace and version path joined with `_`, such as `acme_user_v1_User` for `message("User")` in `"acme.user"` and `version(1)`. It also gets the plain symbol `User` when no other declaration uses that name and no other declaration's qualified symbol has it. A declaration outside any namespace always keeps its plain name. When two declarations share a name, such as `User` in `"one"` and `"two"`, only the qualified symbols `one_User` and `two_User` exist, and compilation warns that the plain name is not available. Two declarations that join to the same qualified name, such as `a_b.C` and `a.b_C`, fail script compilation
- a name built at run time, such as `message(name)` with a variable, gives no symbol. Refer to it with the string form

#### String references

Every typed form has a string form. Use it for names that are built at run time and for short pair declarations.

```kotlin
microsmith {
    schemas {
        protobuf {
            message("GetUserRequest") {
                int32("id") { index(1) }
            }

            message("GetUserResponse") {
                string("email") { index(1) }
            }

            message("WatchUsersRequest")
            message("WatchUsersResponse")

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

A plain name such as `"GetUserResponse"` resolves in the enclosing namespace. A dotted name such as `"acme.shared.PageInfo"` is a full name. Each leading dot goes up one namespace level, so `".v2.UserReply"` inside `acme.user.v1` means `acme.user.v2.UserReply`. A full name can also be typed: `response(MessageRef("acme.shared.PageInfo"))`. A script must import `io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.MessageRef` to write that.

One RPC uses either `request`/`response` or the `to` shorthand, not both.

### ASP.NET service generation DSL

```kotlin
microsmith {
    services {
        dotnet {
            target(NET10)
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

`target(...)` accepts `NET10` and `NET11`.

REST DSL contract notes:

- `asp { ... }` is the canonical surface; `aspNet { ... }` is supported as an alias when the longer spelling reads better in a script
- route groups can be nested, verb helpers are lower-case, and every endpoint requires an explicit operation name
- models are referred to by name, as in `ok("User")`; the .NET DSL has no typed model references yet
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
```

Canonical generation policy:

- `Program.cs` uses top-level hosting and delegates ASP.NET registration through generated hosting extensions
- generated files under `Generated/` provide the contract records, abstract controller base, and response/result mapping surface derived from the normalized REST model
- handwritten service behavior belongs outside `Generated/`, typically in a user-authored controller that derives from the generated base type
- generator-owned scaffold files such as `Program.cs`, `appsettings.json`, `Properties/launchSettings.json`, and everything under `Generated/` are overwritten in place on rerun
- handwritten files outside those generator-owned paths are not overwritten by Microsmith

### Script defaults

Inside `.microsmith.kts` scripts:

- default imports include `microsmith {}`, `services {}`, `.NET`/ASP.NET entrypoints, package helpers, `schemas {}`, and `protobuf {}`
- a script can return a `MicrosmithModel`
- a script can also call `emit(model)` or `generate(model)`

[Back to the README](../README.md)
