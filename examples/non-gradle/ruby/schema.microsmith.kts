microsmith {
    schemas {
        protobuf {
            message("RubyUserCreated") {
                int32("id") { index(1) }
                string("email") { index(2) }
            }
        }
    }
}
