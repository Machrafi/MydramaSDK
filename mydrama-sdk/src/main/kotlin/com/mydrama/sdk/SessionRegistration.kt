package com.mydrama.sdk

/**
 * Result returned by the Worker's `/api/session/register` endpoint.
 */
data class SessionRegistration @JvmOverloads constructor(
    val registered: Boolean,
    val sessionToken: String,
    val expiresAtEpochMillis: Long,
    val mode: String? = null,
    val provider: String? = null,
    val gatePassed: Boolean? = null,
    val riskLevel: String? = null,
)
