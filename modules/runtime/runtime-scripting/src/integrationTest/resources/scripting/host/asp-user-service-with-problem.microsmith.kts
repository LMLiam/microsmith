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
                            get("/{id}", "GetUser") {
                                path("GetUserPath") {
                                    string("id")
                                }
                                responses {
                                    ok("User")
                                    notFound("Problem")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
