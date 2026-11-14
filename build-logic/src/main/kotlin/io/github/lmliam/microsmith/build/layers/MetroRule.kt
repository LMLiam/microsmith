package io.github.lmliam.microsmith.build.layers

private val metroPackage = JvmPackage("dev/zacsweers/metro")

internal fun metroViolations(abiDumps: Map<String, List<String>>): List<ModuleLayerViolation> =
    abiDumps.toSortedMap().flatMap { (module, dumpLines) ->
        if (!isPublishedApiModule(module)) return@flatMap emptyList()
        referencedClasses(dumpLines)
            .filter { it.isIn(metroPackage) }
            .map { ModuleLayerViolation.MetroTypeInPublishedApi(module, it) }
    }
