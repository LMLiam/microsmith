import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import thirdparty.common.FixtureTopics.fixtureTopic
import thirdparty.contract.FixtureExtension

fixtureTopic("Alpha")

check(Alpha.name == "Alpha")

generate(MicrosmithBuilder().apply { put(FixtureExtension::class, FixtureExtension("leak")) }.model)
