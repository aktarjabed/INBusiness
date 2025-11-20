package com.aktarjabed.inbusiness.util

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration

class CircuitBreaker(
    private val failureThreshold: Int = 3,
    private val resetTimeout: Duration,
    private val halfOpenMaxAttempts: Int = 2
) {
    private val mutex = Mutex()
    private var state = State.CLOSED
    private val failureCount = AtomicInteger(0)
    private val successCount = AtomicInteger(0)
    private val lastFailureTime = AtomicLong(0)

    suspend fun <T> execute(block: suspend () -> T): Result<T> = mutex.withLock {
        when (state) {
            State.OPEN -> {
                val timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get()
                if (timeSinceLastFailure > resetTimeout.inWholeMilliseconds) {
                    Log.d(TAG, "Circuit transitioning: OPEN -> HALF_OPEN")
                    state = State.HALF_OPEN
                    successCount.set(0)
                    failureCount.set(0)
                } else {
                    return Result.failure(
                        CircuitBreakerOpenException(
                            "Circuit breaker is OPEN. Retry in ${
                                (resetTimeout.inWholeMilliseconds - timeSinceLastFailure) / 1000
                            } seconds"
                        )
                    )
                }
            }
            State.HALF_OPEN -> {
                if (failureCount.get() >= halfOpenMaxAttempts) {
                    Log.w(TAG, "Circuit reopening: HALF_OPEN -> OPEN (failures exceeded)")
                    state = State.OPEN
                    lastFailureTime.set(System.currentTimeMillis())
                    return Result.failure(
                        CircuitBreakerOpenException("Circuit breaker reopened")
                    )
                }
            }
            State.CLOSED -> {
                // Normal operation
            }
        }

        return try {
            val result = block()
            onSuccess()
            Result.success(result)
        } catch (e: Exception) {
            onFailure(e)
            Result.failure(e)
        }
    }

    private fun onSuccess() {
        when (state) {
            State.HALF_OPEN -> {
                val successes = successCount.incrementAndGet()
                if (successes >= halfOpenMaxAttempts) {
                    Log.i(TAG, "Circuit closing: HALF_OPEN -> CLOSED")
                    state = State.CLOSED
                    failureCount.set(0)
                }
            }
            else -> {
                failureCount.set(0)
            }
        }
    }

    private fun onFailure(exception: Exception) {
        val failures = failureCount.incrementAndGet()
        lastFailureTime.set(System.currentTimeMillis())

        Log.w(TAG, "Circuit breaker failure #$failures: ${exception.message}")

        when (state) {
            State.CLOSED -> {
                if (failures >= failureThreshold) {
                    Log.e(TAG, "Circuit opening: CLOSED -> OPEN (threshold reached)")
                    state = State.OPEN
                }
            }
            State.HALF_OPEN -> {
                Log.w(TAG, "Circuit reopening: HALF_OPEN -> OPEN (failure in test)")
                state = State.OPEN
            }
            State.OPEN -> {
                // Already open, update last failure time
            }
        }
    }

    fun getState(): State = state

    enum class State {
        CLOSED,  // Normal operation
        OPEN,    // Blocking calls
        HALF_OPEN // Testing if service recovered
    }

    companion object {
        private const val TAG = "CircuitBreaker"
    }
}

class CircuitBreakerOpenException(message: String) : Exception(message)