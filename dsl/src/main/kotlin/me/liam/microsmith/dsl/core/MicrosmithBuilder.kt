package me.liam.microsmith.dsl

internal class MicrosmithBuilder : MicrosmithScope {
    private var model = MicrosmithModel.empty()

    inline fun <reified T : RootExtension> put(ext: T) {
        model = model.with<T>(ext)
    }

    fun build() = model
}