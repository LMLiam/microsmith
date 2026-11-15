microsmith {
    schemas {
        protobuf {
            service("ProcessService") {
                "Process" {
                    request(ProcessRequest)
                    response(ProcessResponse)
                }
            }

            message("ProcessRequest") {
                int32("id") { index(1) }
            }

            message("ProcessResponse") {
                int32("id") { index(1) }
            }
        }
    }
}
