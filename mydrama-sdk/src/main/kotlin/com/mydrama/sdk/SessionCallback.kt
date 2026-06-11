package com.mydrama.sdk

/**
 * Java-friendly asynchronous session registration callback.
 *
 * Callbacks run on the SDK's background session executor.
 */
fun interface SessionCallback {
    fun onComplete(registration: SessionRegistration?, error: Throwable?)
}
