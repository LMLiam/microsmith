microsmith {
    schemas {
        protobuf {
            message("JavaUserCreated") {
                int32("id") { index(1) }
                string("email") { index(2) }
            }
        }
    }
}
