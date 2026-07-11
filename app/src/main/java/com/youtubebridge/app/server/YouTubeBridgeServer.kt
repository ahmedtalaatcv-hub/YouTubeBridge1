package com.youtubebridge.app.server

import com.youtubebridge.app.extractor.YoutubeStreamExtractor
import com.youtubebridge.app.util.NetworkUtils
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Embedded, thread-safe local HTTP server.
 *
 * Endpoint:
 *   GET /youtube?url=<youtube watch url>
 *   -> { "youtube": "...", "title": "...", "stream": "...", "updated": 123456789 }
 *
 * Design notes:
 *  - NanoHTTPD spins up a new thread per request (via its default thread pool),
 *    so multiple simultaneous requests are supported out of the box.
 *  - Every response includes permissive CORS headers so any player/browser on the
 *    LAN can call it directly.
 *  - Only requests originating from a private/LAN IP are served; anything else is
 *    rejected with 403, enforcing "same Wi-Fi network only" access.
 */
class YouTubeBridgeServer(
    port: Int,
    private val listener: Listener
) : NanoHTTPD(port) {

    interface Listener {
        fun onLog(message: String, isError: Boolean = false)
        fun onRequestHandled(clientIp: String)
    }

    private val requestCount = AtomicInteger(0)
    private val knownClients = ConcurrentHashMap.newKeySet<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val totalRequests: Int get() = requestCount.get()
    val connectedClientsCount: Int get() = knownClients.size

    override fun serve(session: IHTTPSession): Response {
        val clientIp = session.remoteIpAddress ?: "unknown"

        // --- Security: same-Wi-Fi-network only ---
        if (!NetworkUtils.isLocalNetworkAddress(clientIp)) {
            listener.onLog("رفض اتصال من خارج الشبكة المحلية: $clientIp", isError = true)
            return jsonResponse(Response.Status.FORBIDDEN, errorJson("Forbidden: local network only"))
        }

        requestCount.incrementAndGet()
        knownClients.add(clientIp)
        listener.onRequestHandled(clientIp)

        // Preflight CORS
        if (session.method == Method.OPTIONS) {
            return withCors(newFixedLengthResponse(Response.Status.OK, "text/plain", ""))
        }

        return when {
            session.uri == "/youtube" -> handleYoutube(session, clientIp)
            session.uri == "/status" -> handleStatus()
            else -> jsonResponse(Response.Status.NOT_FOUND, errorJson("Not found"))
        }
    }

    private fun handleYoutube(session: IHTTPSession, clientIp: String): Response {
        val url = session.parameters["url"]?.firstOrNull()

        if (url.isNullOrBlank()) {
            listener.onLog("طلب من $clientIp بدون معامل url", isError = true)
            return jsonResponse(Response.Status.BAD_REQUEST, errorJson("Missing 'url' query parameter"))
        }

        return try {
            val result = runBlocking { YoutubeStreamExtractor.resolve(url) }
            listener.onLog("تم استخراج الرابط بنجاح لـ: ${result.title}")

            val json = JSONObject().apply {
                put("youtube", result.youtube)
                put("title", result.title)
                put("stream", result.stream)
                put("updated", result.updated)
            }
            jsonResponse(Response.Status.OK, json)
        } catch (e: Exception) {
            listener.onLog("فشل استخراج الرابط: ${e.message}", isError = true)
            jsonResponse(Response.Status.INTERNAL_ERROR, errorJson(e.message ?: "Extraction failed"))
        }
    }

    private fun handleStatus(): Response {
        val json = JSONObject().apply {
            put("status", "running")
            put("requests", totalRequests)
            put("clients", connectedClientsCount)
        }
        return jsonResponse(Response.Status.OK, json)
    }

    private fun errorJson(message: String): JSONObject =
        JSONObject().apply { put("error", message) }

    private fun jsonResponse(status: Response.Status, json: JSONObject): Response {
        val response = newFixedLengthResponse(status, "application/json; charset=utf-8", json.toString())
        return withCors(response)
    }

    private fun withCors(response: Response): Response {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "*")
        return response
    }
}
