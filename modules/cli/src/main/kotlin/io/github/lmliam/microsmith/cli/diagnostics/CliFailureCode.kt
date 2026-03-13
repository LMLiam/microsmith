package io.github.lmliam.microsmith.cli.diagnostics

internal enum class CliFailureCode(val id: String, val exitCode: Int) {
    USAGE_ERROR(id = "MS-CLI-0001", exitCode = 2),
    PROVIDER_VALIDATION_FAILED(id = "MS-CLI-1001", exitCode = 10),
    PLUGIN_RESOLUTION_FAILED(id = "MS-CLI-1101", exitCode = 11),
    SCRIPT_VALIDATION_FAILED(id = "MS-CLI-2001", exitCode = 20),
    SCRIPT_COMPILATION_FAILED(id = "MS-CLI-2002", exitCode = 21),
    SCRIPT_EVALUATION_FAILED(id = "MS-CLI-2003", exitCode = 22),
    SCRIPT_HOST_FAILED(id = "MS-CLI-2004", exitCode = 23),
    DOCTOR_FAILED(id = "MS-CLI-3001", exitCode = 30),
    IDE_HELPER_FAILED(id = "MS-CLI-4001", exitCode = 40),
    IDE_DOCTOR_FAILED(id = "MS-CLI-4101", exitCode = 41),
    INIT_CONFLICT(id = "MS-CLI-5001", exitCode = 50),
    INIT_VALIDATION_FAILED(id = "MS-CLI-5002", exitCode = 51),
    INIT_RUNTIME_FAILED(id = "MS-CLI-5003", exitCode = 52),
}
