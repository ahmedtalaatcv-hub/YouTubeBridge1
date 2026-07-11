package com.youtubebridge.app.extractor

import com.youtubebridge.app.model.StreamResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo

/**
 * Resolves a public YouTube watch URL into direct, playable stream info.
 *
 * IMPORTANT TECHNICAL NOTE (read before relying on this in production):
 * There is no official Android-native equivalent of yt-dlp. yt-dlp itself is a Python
 * program and cannot run inside an Android process without embedding a Python runtime
 * (e.g. Chaquopy), which is heavy, fragile to keep updated, and still legally identical
 * in what it does. The practical, maintained, pure-JVM alternative used here is
 * NewPipeExtractor (https://github.com/TeamNewPipe/NewPipeExtractor) — the same library
 * that powers the NewPipe app. It parses YouTube's internal player API directly in
 * Kotlin/Java, requires no Python/Node, and returns direct progressive (audio+video)
 * or adaptive stream URLs.
 *
 * Caveats that are inherent to ANY unofficial extraction approach (yt-dlp included),
 * not specific to this implementation:
 *  - YouTube changes its player internals periodically; extraction can break until the
 *    extractor library is updated (menu option "تحديث أداة استخراج روابط YouTube" bumps
 *    the dependency version).
 *  - Resolved URLs are typically short-lived (signed, expire after a few hours) and are
 *    tied to the requesting IP/User-Agent in some cases — this is why the response
 *    includes an "updated" timestamp so the IPTV player can re-fetch when needed.
 *  - Age-restricted, region-locked, or DRM-protected videos may fail to resolve.
 *  - This uses YouTube in a way that is outside YouTube's official API terms of
 *    service, exactly like yt-dlp/NewPipe. Use for personal, non-commercial purposes.
 */
object YoutubeStreamExtractor {

    private const val YOUTUBE_SERVICE_ID = 0 // ServiceList.YouTube.serviceId

    @Volatile
    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    NewPipe.init(SimpleDownloader.instance)
                    initialized = true
                }
            }
        }
    }

    suspend fun resolve(youtubeUrl: String): StreamResult = withContext(Dispatchers.IO) {
        ensureInitialized()

        val normalizedUrl = normalize(youtubeUrl)
        val info: StreamInfo = StreamInfo.getInfo(ServiceList.YouTube, normalizedUrl)

        // Prefer a progressive (single URL, audio+video combined) stream so it can be
        // dropped straight into an HTML5 <video> tag / most IPTV players without
        // needing separate audio+video muxing.
        val progressive = info.videoStreams
            ?.filter { !it.isVideoOnly && it.url != null }
            ?.maxByOrNull { it.height }

        val fallback = info.videoOnlyStreams
            ?.filter { it.url != null }
            ?.maxByOrNull { it.height }

        val directUrl = progressive?.url
            ?: fallback?.url
            ?: info.hlsUrl
            ?: throw IllegalStateException("لا يوجد رابط بث مباشر متاح لهذا الفيديو")

        StreamResult(
            youtube = normalizedUrl,
            title = info.name ?: "",
            stream = directUrl,
            updated = System.currentTimeMillis() / 1000
        )
    }

    private fun normalize(url: String): String {
        var u = url.trim()
        if (!u.startsWith("http")) u = "https://$u"
        return u
    }
}
