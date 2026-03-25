package io.github.lmliam.microsmith.resolve.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

private data object AlphaExtension : MicrosmithExtension

private data object BetaExtension : MicrosmithExtension

private data class AlphaResolvedModel(
    val value: String,
) : ResolvedModel

private data class BetaResolvedModel(
    val value: String,
) : ResolvedModel

private class AlphaFirstResolver : DomainResolver<AlphaExtension, AlphaResolvedModel> {
    override val authoringType: KClass<AlphaExtension> = AlphaExtension::class
    override val resolvedType: KClass<AlphaResolvedModel> = AlphaResolvedModel::class

    override fun resolve(authoring: AlphaExtension) = AlphaResolvedModel("alpha")
}

private class AlphaNullResolver : DomainResolver<AlphaExtension, BetaResolvedModel> {
    override val authoringType: KClass<AlphaExtension> = AlphaExtension::class
    override val resolvedType: KClass<BetaResolvedModel> = BetaResolvedModel::class

    override fun resolve(authoring: AlphaExtension): BetaResolvedModel? = null
}

private class BetaResolver : DomainResolver<BetaExtension, BetaResolvedModel> {
    override val authoringType: KClass<BetaExtension> = BetaExtension::class
    override val resolvedType: KClass<BetaResolvedModel> = BetaResolvedModel::class

    override fun resolve(authoring: BetaExtension) = BetaResolvedModel("beta")
}

class DomainResolutionServiceTests :
    StringSpec({
        "resolve returns resolved models for matching authoring extensions only" {
            val builder = MicrosmithBuilder().apply {
                put(BetaExtension::class, BetaExtension)
                put(AlphaExtension::class, AlphaExtension)
            }
            val service =
                DomainResolutionService(
                    DomainResolverRegistry(
                        listOf(
                            BetaResolver(),
                            AlphaNullResolver(),
                            AlphaFirstResolver(),
                        ),
                    ),
                )

            service.resolve(builder.model) shouldContainExactly listOf(
                AlphaResolvedModel("alpha"),
                BetaResolvedModel("beta"),
            )
        }

        "registry sorts resolvers for an authoring type deterministically by resolved type then implementation" {
            val registry =
                DomainResolverRegistry(
                    listOf(
                        BetaResolver(),
                        AlphaFirstResolver(),
                        AlphaNullResolver(),
                    ),
                )

            val resolvers = registry.resolve(AlphaExtension)

            resolvers.map { it.resolvedType } shouldContainExactly listOf(
                AlphaResolvedModel::class,
                BetaResolvedModel::class,
            )
            resolvers.first().authoringType shouldBe AlphaExtension::class
        }
    })
