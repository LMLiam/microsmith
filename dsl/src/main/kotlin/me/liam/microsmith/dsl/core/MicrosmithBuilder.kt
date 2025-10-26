package me.liam.microsmith.dsl.core

/**
 * Internal builder used to construct a [MicrosmithModel] from DSL blocks.
 *
 * - End-users never see this class directly.
 * - Plugin authors may downcast [MicrosmithScope] to [MicrosmithBuilder] if they
 *   want to attach their own [MicrosmithExtension]s.
 *
 * The builder maintains a mutable reference to the current model, but the model
 * itself is immutable. Each [put] call produces a new [MicrosmithModel].
 */
internal class MicrosmithBuilder : MicrosmithScope {
    /**
     * The current immutable model snapshot being built.
     */
    var model = MicrosmithModel.empty()
        private set

    /**
     * Attach a new [MicrosmithExtension] to the model.
     *
     * If an extension of the same type already exists, it will be replaced.
     */
    inline fun <reified T : MicrosmithExtension> put(ext: T) {
        model = model.with<T>(ext)
    }
}