package com.drinkit.search.engine

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.nulls.shouldBeNull
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

// A rebuild returns at once, and its batches are handled on other threads
internal fun SearchIndex<*>.awaitRebuilt() = runBlocking {
    eventually(10.seconds) { rebuildProgress().shouldBeNull() }
}
