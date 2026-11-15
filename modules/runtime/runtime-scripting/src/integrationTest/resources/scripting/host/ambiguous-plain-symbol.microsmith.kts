microsmith {
    schemas {
        protobuf {
            "one" {
                message("User") {
                    int32("id") { index(1) }
                }
            }

            "two" {
                message("User") {
                    int32("id") { index(1) }
                }
            }

            service("Users") {
                "Get" {
                    request(User)
                    response(two_User)
                }
            }
        }
    }
}
