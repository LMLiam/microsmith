microsmith {
    schemas {
        protobuf {
            message("KotlinGradleUserCreated") {
                int32("id") { index(1) }
                string("email") { index(2) }
            }
        }
    }
}
