package me.liam.microsmith.runtime.scripting.host

internal object ProcessIsolationPropertyNames {
    const val REQUEST_SCRIPT = "request.script"
    const val REQUEST_OUTPUT_DIR = "request.outputDir"
    const val REQUEST_CACHE_DIR = "request.cacheDir"
    const val REQUEST_PLUGIN_CLASSPATH_COUNT = "request.pluginClasspath.count"
    const val REQUEST_PLUGIN_CLASSPATH_PREFIX = "request.pluginClasspath."
    const val REQUEST_VARIABLE_COUNT = "request.variables.count"
    const val REQUEST_VARIABLE_KEY_PREFIX = "request.variables.key."
    const val REQUEST_VARIABLE_VALUE_PREFIX = "request.variables.value."
    const val REQUEST_FLAG_COUNT = "request.flags.count"
    const val REQUEST_FLAG_PREFIX = "request.flags."

    const val RESULT_STATUS = "result.status"
    const val RESULT_STATUS_SUCCESS = "success"
    const val RESULT_STATUS_FAILURE = "failure"
    const val RESULT_ELAPSED_MILLIS = "result.elapsedMillis"
    const val RESULT_CACHE_HIT = "result.cacheHit"
    const val RESULT_WARNING_COUNT = "result.warnings.count"
    const val RESULT_WARNING_PREFIX = "result.warnings."
    const val RESULT_DIAGNOSTIC_COUNT = "result.diagnostics.count"
    const val RESULT_DIAGNOSTIC_PREFIX = "result.diagnostics."
    const val RESULT_FAILURE_TYPE = "result.failure.type"
}
