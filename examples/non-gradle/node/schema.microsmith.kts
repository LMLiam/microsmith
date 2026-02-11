microsmith {
    schemas {
        protobuf {
            message("NodeUserCreated") {
                int32("id") { index(1) }
                string("email") { index(2) }
            }
        }
    }
}
