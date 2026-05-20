package dev.karipap.app.input.autoconfig

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async

@OptIn(ExperimentalCoroutinesApi::class)
class BundledAutoconfigEntries private constructor(
    private val deferred: Deferred<List<RetroArchCfgEntry>>?,
    private val eager: List<RetroArchCfgEntry>?,
) {
    constructor(load: () -> List<RetroArchCfgEntry>) : this(
        deferred = CoroutineScope(SupervisorJob() + Dispatchers.IO).async { load() },
        eager = null,
    )

    private val pendingOnLoaded = mutableListOf<() -> Unit>()

    init {
        deferred?.invokeOnCompletion {
            Handler(Looper.getMainLooper()).post {
                val callbacks: List<() -> Unit>
                synchronized(pendingOnLoaded) {
                    callbacks = pendingOnLoaded.toList()
                    pendingOnLoaded.clear()
                }
                callbacks.forEach { it() }
            }
        }
    }

    fun entries(): List<RetroArchCfgEntry> {
        if (eager != null) return eager
        val d = deferred ?: return emptyList()
        return if (d.isCompleted) d.getCompleted() else emptyList()
    }

    fun onLoaded(action: () -> Unit) {
        if (eager != null || deferred?.isCompleted == true) {
            action()
            return
        }
        synchronized(pendingOnLoaded) { pendingOnLoaded.add(action) }
    }

    companion object {
        fun forTest(entries: List<RetroArchCfgEntry>): BundledAutoconfigEntries =
            BundledAutoconfigEntries(deferred = null, eager = entries)
    }
}
