package com.youtubebridge.app.extractor

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * Minimal [Downloader] implementation for NewPipeExtractor built on top of
 * java.net.HttpURLConnection, so the app needs no extra HTTP client dependency.
 */
class SimpleDownloader private constructor() : Downloader() {

    override fun execute(request: Request): Response {
        val url = URL(request.url())
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = request.httpMethod()
            connectTimeout = 15000
            readTimeout = 15000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            request.headers().forEach { (key, values) ->
                values.forEach { value -> addRequestProperty(key, value) }
            }
        }

        request.dataToSend()?.let { body ->
            connection.doOutput = true
            connection.outputStream.use { it.write(body) }
        }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val encoding = connection.contentEncoding

        val bytes = try {
            val actualStream = if (encoding == "gzip") GZIPInputStream(stream) else stream
            actualStream.use { it.readBytes() }
        } catch (e: IOException) {
            ByteArray(0)
        }

        val body = String(bytes, Charsets.UTF_8)
        val headers = connection.headerFields.mapValues { it.value ?: emptyList() }

        return Response(
            code,
            connection.responseMessage ?: "",
            headers,
            body,
            connection.url.toString()
        )
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

        val instance: SimpleDownloader by lazy { SimpleDownloader() }
    }
}
