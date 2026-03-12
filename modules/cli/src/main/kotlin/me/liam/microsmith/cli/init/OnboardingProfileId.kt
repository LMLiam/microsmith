package me.liam.microsmith.cli.init

@JvmInline
internal value class OnboardingProfileId(
    val value: String,
) {
    override fun toString(): String = value
}
