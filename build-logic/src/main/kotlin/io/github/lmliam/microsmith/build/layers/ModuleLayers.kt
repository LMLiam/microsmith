package io.github.lmliam.microsmith.build.layers

internal fun findModuleLayerViolations(
    projectDependencies: Map<String, Collection<String>>,
    abiDumps: Map<String, List<String>>,
): List<ModuleLayerViolation> = dependencyViolations(projectDependencies) + metroViolations(abiDumps)
