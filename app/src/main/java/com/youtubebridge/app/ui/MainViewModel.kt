package com.youtubebridge.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.youtubebridge.app.model.ServerUiState
import com.youtubebridge.app.service.ServerForegroundService
import com.youtubebridge.app.util.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    private val _uiState = MutableStateFlow(
        ServerUiState(autoStart = prefs.autoStart, darkMode = prefs.darkMode, port = prefs.port)
    )
    val uiState: StateFlow<ServerUiState> = _uiState.asStateFlow()

    private var boundService: ServerForegroundService? = null

    fun onServiceConnected(service: ServerForegroundService) {
        boundService = service
        viewModelScope.launch {
            service.state.collect { s ->
                _uiState.value = _uiState.value.copy(
                    isRunning = s.isRunning,
                    ipAddress = s.ipAddress,
                    port = s.port,
                    requestCount = s.requestCount,
                    connectedClients = s.connectedClients,
                    logs = s.logs
                )
            }
        }
    }

    fun onServiceDisconnected() {
        boundService = null
    }

    fun startServer() = boundService?.startServer()
    fun stopServer() = boundService?.stopServer()
    fun restartServer() = boundService?.restartServer()
    fun refreshIp() = boundService?.refreshIp()

    fun setPort(newPort: Int) {
        prefs.port = newPort
        _uiState.value = _uiState.value.copy(port = newPort)
    }

    fun setAutoStart(enabled: Boolean) {
        prefs.autoStart = enabled
        _uiState.value = _uiState.value.copy(autoStart = enabled)
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.darkMode = enabled
        _uiState.value = _uiState.value.copy(darkMode = enabled)
    }

    fun clearLogs() {
        _uiState.value = _uiState.value.copy(logs = emptyList())
    }
}
