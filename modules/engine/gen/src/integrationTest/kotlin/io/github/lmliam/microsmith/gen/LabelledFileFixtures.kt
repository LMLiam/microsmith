package io.github.lmliam.microsmith.gen

import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.files.BinaryFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.BinaryFileArtifactId
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.MicrosmithModel
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver
import io.github.lmliam.microsmith.resolve.ResolvedModel
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

data object LabelExtension : MicrosmithExtension

data class LabelledModel(val label: String) : ResolvedModel

object ResolutionRendezvous {
    @Volatile var barrier: CyclicBarrier? = null

    fun await() {
        barrier?.await(RENDEZVOUS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private const val RENDEZVOUS_TIMEOUT_SECONDS = 30L
}

abstract class LabelResolver(private val label: String) : DomainResolver<LabelExtension, LabelledModel> {
    override val authoringType = LabelExtension::class
    override val resolvedType = LabelledModel::class

    override fun resolve(authoring: LabelExtension): DomainResolution<LabelledModel> {
        ResolutionRendezvous.await()
        return DomainResolution.Success(LabelledModel(label))
    }
}

class FirstLabelResolver : LabelResolver("first")

class SecondLabelResolver : LabelResolver("second")

class LabelFileContributor : ArtifactContributor<LabelledModel> {
    override val resolvedType = LabelledModel::class

    override fun contribute(model: LabelledModel): List<ArtifactContribution<Artifact>> =
        listOf(
            TextFileArtifactContribution(TextFileArtifactId(RelativePath.of("label.txt")), model.label),
            BinaryFileArtifactContribution(
                BinaryFileArtifactId(RelativePath.of("label.bin")),
                model.label.toByteArray(),
            ),
        )
}

fun labelledModel(): MicrosmithModel = MicrosmithBuilder().apply { put(LabelExtension::class, LabelExtension) }.model

fun labelClassLoader(resolver: kotlin.reflect.KClass<out LabelResolver>): ClassLoader =
    ServiceProviderClassLoader(
        parent = LabelResolver::class.java.classLoader,
        registrations =
            mapOf(
                DomainResolver::class to listOf(resolver),
                ArtifactContributor::class to listOf(LabelFileContributor::class),
            ),
    )
