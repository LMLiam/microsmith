package io.github.lmliam.microsmith.gen.output

import io.github.lmliam.microsmith.artifact.files.RelativePath
import java.nio.file.FileSystem
import java.nio.file.Path

internal fun FileSystem.pathOf(path: RelativePath): Path =
    path.segments.fold(getPath("")) { current, segment -> current.resolve(segment) }
