microsmith {
    schemas {
        protobuf {
            message("KotlinUserCreated") {
                int32("id") { index(1) }
                string("email") { index(2) }
            }
        }
    }
}
