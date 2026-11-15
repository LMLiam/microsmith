val schemaName = requireVar("schema")
if (!hasFlag("emit")) {
    error("emit flag is required")
}

val model =
    microsmith {
        schemas {
            protobuf {
                message(schemaName) {
                    int32("id") { index(1) }
                }
            }
        }
    }
emit(model)
