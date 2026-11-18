package com.soniq.player.core.common

import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes [block] and captures failures in a [Result] **without** swallowing coroutine
 * cancellation. `kotlin.runCatching` must not be used in suspending code because it would turn
 * a [CancellationException] into a failure value and break structured concurrency.
 */
suspend inline fun <T> suspendRunCatching(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (@Suppress("TooGenericExceptionCaught") throwable: Exception) {
        Result.failure(throwable)
    }

/**
 * Parses a persisted enum [name] defensively. Persisted data can outlive the code that wrote it
 * (renamed or removed constants, downgrades, corrupted values), so unknown values fall back to
 * [default] instead of crashing.
 */
inline fun <reified E : Enum<E>> enumValueOrDefault(name: String?, default: E): E =
    if (name == null) default else enumValues<E>().firstOrNull { it.name == name } ?: default
