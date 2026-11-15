import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import thirdparty.contract.FixtureExtension

generate(MicrosmithBuilder().apply { put(FixtureExtension::class, FixtureExtension("stalling")) }.model)
