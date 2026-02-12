microsmith {
    schemas {
        protobuf {
            message("DotnetUserCreated") {
                int32("id") { index(1) }
                string("email") { index(2) }
            }
        }
    }
}
