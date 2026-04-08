microsmith {
    schemas {
        protobuf {
            message("DotnetUserCreated") {
                int32("id") { index(1) }
                string("email") { index(2) }
            }
        }
    }

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
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
