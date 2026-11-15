microsmith {
    schemas {
        protobuf {
            enum("Role") {
                +"USER"
            }

            service("Users") {
                "GetUsers" {
                    request(Role)
                    response(Role)
                }
            }
        }
    }
}
