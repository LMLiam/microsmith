microsmith {
    schemas {
        protobuf {
            message("RustUserCreated") {
                int32("id") { index(1) }
                string("email") { index(2) }
            }
        }
    }
}
