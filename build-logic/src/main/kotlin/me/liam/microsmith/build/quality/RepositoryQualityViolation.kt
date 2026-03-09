package me.liam.microsmith.build.quality

internal data class RepositoryQualityViolation(
    val rule: String,
    val path: String,
    val message: String,
)
