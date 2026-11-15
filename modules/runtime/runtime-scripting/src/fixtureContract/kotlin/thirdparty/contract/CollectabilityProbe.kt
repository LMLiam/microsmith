package thirdparty.contract

import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

const val STUBBORN_THREAD_NAME = "thirdparty-stubborn-thread"

object CollectabilityProbe {
    private val trackedInstances = CopyOnWriteArrayList<WeakReference<Any>>()
    private val trackedClasses = CopyOnWriteArrayList<WeakReference<Class<*>>>()
    private val trackedLoaders = CopyOnWriteArrayList<WeakReference<ClassLoader>>()

    @Volatile var stopRequested = false

    val instances: List<WeakReference<Any>>
        get() = trackedInstances.toList()

    val classes: List<WeakReference<Class<*>>>
        get() = trackedClasses.toList()

    val loaders: List<WeakReference<ClassLoader>>
        get() = trackedLoaders.toList()

    fun track(instance: Any) {
        trackedInstances += WeakReference(instance)
        trackedClasses += WeakReference(instance.javaClass)
        trackedLoaders += WeakReference(instance.javaClass.classLoader)
    }

    fun reset() {
        trackedInstances.clear()
        trackedClasses.clear()
        trackedLoaders.clear()
        stopRequested = false
    }
}
