microsmith {
    schemas {
        protobuf {
            service("Users") {
                "GetUsers" {
                    request(GetUsersRequest)
                    response(GetUsersResponse)
                }
            }

            message("GetUsersRequest") {
                int32("id") { index(1) }
            }

            message("GetUsersResponse") {
                int32("id") { index(1) }
            }
        }
    }
}
