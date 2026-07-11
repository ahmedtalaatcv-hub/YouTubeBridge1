package com.youtubebridge.app.model

/** Result returned to the caller after resolving a YouTube URL into a direct stream. */
data class StreamResult(
    val youtube: String,
    val title: String,
    val stream: String,
    val updated: Long
)

/** A single line shown in the in-app Logs panel. */
data class LogEntry(
    val timestamp: Long,
    val message: String,
    val isError: Boolean = false
)

/** Snapshot of server status consumed by the UI. */
data class ServerUiState(
    val isRunning: Boolean = false,
    val ipAddress: String = "—",
    val port: Int = 3000,
    val requestCount: Int = 0,
    val connectedClients: Int = 0,
    val logs: List<LogEntry> = emptyList(),
    val autoStart: Boolean = true,
    val darkMode: Boolean = true
)
