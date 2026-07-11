package com.youtubebridge.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.youtubebridge.app.App
import com.youtubebridge.app.MainActivity
import com.youtubebridge.app.R
import com.youtubebridge.app.model.LogEntry
import com.youtubebridge.app.server.YouTubeBridgeServer
import com.youtubebridge.app.util.NetworkUtils
import com.youtubebridge.app.util.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Foreground service that owns the lifetime of [YouTubeBridgeServer].
 * Runs as a low-priority persistent notification so Android does not kill it,
 * and restarts itself after boot via [com.youtubebridge.app.receiver.BootReceiver].
 */
class ServerForegroundService : Service(), YouTubeBridgeServer.Listener {

    private val binder = LocalBinder()
    private var server: YouTubeBridgeServer? = null
    private lateinit var prefs: Prefs

    private val _state = MutableStateFlow(ServiceState())
    val state: StateFlow<ServiceState> = _state.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): ServerForegroundService = this@ServerForegroundService
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopServer()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startServer()
        }
        return START_STICKY
    }

    fun startServer() {
        if (server != null) return
        val port = prefs.port
        try {
            server = YouTubeBridgeServer(port, this).also { it.start() }
            val ip = NetworkUtils.getLocalIpAddress(this) ?: "غير متصل"
            _state.update { it.copy(isRunning = true, port = port, ipAddress = ip) }
            startForeground(NOTIFICATION_ID, buildNotification(ip, port))
            addLog("تم تشغيل السيرفر على المنفذ $port")
        } catch (e: Exception) {
            addLog("فشل تشغيل السيرفر: ${e.message}", isError = true)
        }
    }

    fun stopServer() {
        server?.stop()
        server = null
        _state.update { it.copy(isRunning = false) }
        addLog("تم إيقاف السيرفر")
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun restartServer() {
        stopServer()
        startServer()
    }

    fun refreshIp() {
        val ip = NetworkUtils.getLocalIpAddress(this) ?: "غير متصل"
        _state.update { it.copy(ipAddress = ip) }
    }

    // --- YouTubeBridgeServer.Listener ---

    override fun onLog(message: String, isError: Boolean) {
        addLog(message, isError)
    }

    override fun onRequestHandled(clientIp: String) {
        _state.update {
            it.copy(
                requestCount = server?.totalRequests ?: it.requestCount,
                connectedClients = server?.connectedClientsCount ?: it.connectedClients
            )
        }
    }

    private fun addLog(message: String, isError: Boolean = false) {
        _state.update { current ->
            val newLogs = (current.logs + LogEntry(System.currentTimeMillis(), message, isError))
                .takeLast(200)
            current.copy(logs = newLogs)
        }
    }

    private fun buildNotification(ip: String, port: Int): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, ServerForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, App.CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_running, port) + " · $ip")
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, "إيقاف", stopIntent)
            .build()
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    data class ServiceState(
        val isRunning: Boolean = false,
        val ipAddress: String = "—",
        val port: Int = Prefs.DEFAULT_PORT,
        val requestCount: Int = 0,
        val connectedClients: Int = 0,
        val logs: List<LogEntry> = emptyList()
    )

    companion object {
        const val ACTION_STOP = "com.youtubebridge.app.action.STOP"
        private const val NOTIFICATION_ID = 42

        fun start(context: Context) {
            val intent = Intent(context, ServerForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
