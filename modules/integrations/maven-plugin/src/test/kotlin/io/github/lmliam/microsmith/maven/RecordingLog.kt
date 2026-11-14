package io.github.lmliam.microsmith.maven

import org.apache.maven.plugin.logging.SystemStreamLog

internal class RecordingLog : SystemStreamLog() {
    val warnings = mutableListOf<String>()

    override fun warn(content: CharSequence?) {
        warnings += content.toString()
    }
}
