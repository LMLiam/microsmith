package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

@JvmInline
value class MsBuildItemName private constructor(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "MSBuild item name cannot be blank." }
    }

    override fun toString(): String = value

    companion object {
        val PackageReference = MsBuildItemName("PackageReference")
        val PackageVersion = MsBuildItemName("PackageVersion")

        fun of(value: String): MsBuildItemName = MsBuildItemName(value)
    }
}
