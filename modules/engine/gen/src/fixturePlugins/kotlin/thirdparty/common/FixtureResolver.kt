package thirdparty.common

import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver
import thirdparty.contract.FixtureExtension
import thirdparty.contract.FixtureModel

class FixtureResolver : DomainResolver<FixtureExtension, FixtureModel> {
    override val authoringType = FixtureExtension::class
    override val resolvedType = FixtureModel::class

    override fun resolve(authoring: FixtureExtension): DomainResolution<FixtureModel> =
        DomainResolution.Success(FixtureModel(authoring.text))
}
