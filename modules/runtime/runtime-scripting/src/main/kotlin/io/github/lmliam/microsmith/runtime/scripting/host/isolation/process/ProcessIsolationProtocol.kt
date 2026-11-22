package io.github.lmliam.microsmith.runtime.scripting.host.isolation.process

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Path

@Inject
internal class ProcessIsolationProtocol(
    private val requestCodec: ProcessIsolationRequestCodec,
    private val resultCodec: ProcessIsolationResultCodec,
) {
    fun writeRequest(path: Path, request: ProcessIsolationRequest) {
        requestCodec.write(path, request)
    }

    fun readRequest(path: Path): ProcessIsolationRequest = requestCodec.read(path)

    fun writeResult(path: Path, result: ScriptRunResult) {
        resultCodec.write(path, result)
    }

    fun readResult(path: Path): ScriptRunResult = resultCodec.read(path)
}
