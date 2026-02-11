microsmith {
    schemas {
        protobuf {
            message("GoUserCreated") {
                int32("id") { index(1) }
                string("email") { index(2) }
            }
        }
    }
}
