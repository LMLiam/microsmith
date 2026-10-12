package io.github.lmliam.microsmith.resolve

import arrow.core.nonEmptyListOf
import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.MicrosmithExtension
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

private data object AlphaExtension : MicrosmithExtension

private data object BetaExtension : MicrosmithExtension

private data class AlphaResolvedModel(val value: String) : ResolvedModel

private data class BetaResolvedModel(val value: String) : ResolvedModel

private data object AlphaIssue : ResolutionIssue

private data object BetaIssue : ResolutionIssue

private class AlphaFirstResolver : DomainResolver<AlphaExtension, AlphaResolvedModel> {
    override val authoringType = AlphaExtension::class
    override val resolvedType = AlphaResolvedModel::class

    override fun resolve(authoring: AlphaExtension): DomainResolution<AlphaResolvedModel> =
        DomainResolution.Success(AlphaResolvedModel("alpha"))
}

private class AlphaNotApplicableResolver : DomainResolver<AlphaExtension, BetaResolvedModel> {
    override val authoringType = AlphaExtension::class
    override val resolvedType = BetaResolvedModel::class

    override fun resolve(authoring: AlphaExtension): DomainResolution<BetaResolvedModel> =
        DomainResolution.NotApplicable
}

private class BetaResolver : DomainResolver<BetaExtension, BetaResolvedModel> {
    override val authoringType = BetaExtension::class
    override val resolvedType = BetaResolvedModel::class

    override fun resolve(authoring: BetaExtension): DomainResolution<BetaResolvedModel> =
        DomainResolution.Success(BetaResolvedModel("beta"))
}

private class AlphaFailureResolver : DomainResolver<AlphaExtension, AlphaResolvedModel> {
    override val authoringType = AlphaExtension::class
    override val resolvedType = AlphaResolvedModel::class

    override fun resolve(authoring: AlphaExtension): DomainResolution<AlphaResolvedModel> =
        DomainResolution.Failure(nonEmptyListOf(AlphaIssue))
}

private class BetaFailureResolver : DomainResolver<BetaExtension, BetaResolvedModel> {
    override val authoringType = BetaExtension::class
    override val resolvedType = BetaResolvedModel::class

    override fun resolve(authoring: BetaExtension): DomainResolution<BetaResolvedModel> =
        DomainResolution.Failure(nonEmptyListOf(BetaIssue))
}

private class ThrowingResolver : DomainResolver<AlphaExtension, AlphaResolvedModel> {
    override val authoringType = AlphaExtension::class
    override val resolvedType = AlphaResolvedModel::class

    override fun resolve(authoring: AlphaExtension): DomainResolution<AlphaResolvedModel> =
        error("infrastructure exploded")
}

class DomainResolutionServiceTests :
    StringSpec({
        "resolve returns resolved models for successful matching resolvers" {
            val builder =
                MicrosmithBuilder().apply {
                    put(BetaExtension::class, BetaExtension)
                    put(AlphaExtension::class, AlphaExtension)
                }

            val service =
                DomainResolutionService(
                    listOf(
                        BetaResolver(),
                        AlphaNotApplicableResolver(),
                        AlphaFirstResolver(),
                    )
                )

            val result = service.resolve(builder.model).shouldBeTypeOf<ResolutionOutcome.Success>()

            result.models shouldContainExactly
                listOf(
                    AlphaResolvedModel("alpha"),
                    BetaResolvedModel("beta"),
                )
        }

        "resolve accumulates semantic issues across matching resolvers" {
            val builder =
                MicrosmithBuilder().apply {
                    put(BetaExtension::class, BetaExtension)
                    put(AlphaExtension::class, AlphaExtension)
                }

            val service =
                DomainResolutionService(
                    listOf(
                        BetaFailureResolver(),
                        AlphaFailureResolver(),
                    )
                )

            val result = service.resolve(builder.model).shouldBeTypeOf<ResolutionOutcome.Failure>()

            result.issues.toList() shouldContainExactly
                listOf(
                    AlphaIssue,
                    BetaIssue,
                )
        }

        "resolve does not convert unexpected exceptions into semantic issues" {
            val builder = MicrosmithBuilder().apply { put(AlphaExtension::class, AlphaExtension) }

            val service = DomainResolutionService(listOf(ThrowingResolver()))

            shouldThrow<IllegalStateException> { service.resolve(builder.model) }.message shouldBe
                "infrastructure exploded"
        }

        "registry sorts resolvers for an authoring type deterministically by resolved type then implementation" {
            val registry =
                DomainResolverRegistry(
                    listOf(
                        BetaResolver(),
                        AlphaFirstResolver(),
                        AlphaNotApplicableResolver(),
                    )
                )

            val resolvers = registry.resolve(AlphaExtension)

            resolvers.map { it.resolvedType } shouldContainExactly
                listOf(
                    AlphaResolvedModel::class,
                    BetaResolvedModel::class,
                )

            resolvers.first().authoringType shouldBe AlphaExtension::class
        }
    })
