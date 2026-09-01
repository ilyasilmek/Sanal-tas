package com.example.data.sync

import android.util.Log
import com.example.data.security.AntiCheat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class RockWebSocketSync(
    private val scope: CoroutineScope,
    private var serverUrl: String = "ws://10.0.2.2:8080"
) {
    private val TAG = "RockWebSocketSync"

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var simulationJob: Job? = null

    private val _serverState = MutableStateFlow(SyncServerState(serverUrl = serverUrl))
    val serverState: StateFlow<SyncServerState> = _serverState.asStateFlow()

    private var isSimulating = false

    fun start() {
        connect()
        startPeriodicSimulationFallback()
    }

    fun updateServerUrl(newUrl: String) {
        if (serverUrl == newUrl) return
        serverUrl = newUrl
        _serverState.update { it.copy(serverUrl = newUrl) }
        disconnect()
        connect()
    }

    private fun connect() {
        if (_serverState.value.isConnected) return

        try {
            val request = Request.Builder()
                .url(serverUrl)
                .addHeader("x-country-code", "TR")
                .build()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket Connected to $serverUrl")
                    isSimulating = false
                    _serverState.update {
                        it.copy(
                            isConnected = true,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleIncomingMessage(text)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket Closed: $code - $reason")
                    _serverState.update { it.copy(isConnected = false) }
                    scheduleReconnect()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "WebSocket Failure: ${t.message}. Falling back to local offline mode.")
                    _serverState.update { it.copy(isConnected = false) }
                    scheduleReconnect()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect WebSocket: ${e.message}")
            _serverState.update { it.copy(isConnected = false) }
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch(Dispatchers.IO) {
            delay(5000)
            if (!_serverState.value.isConnected) {
                connect()
            }
        }
    }

    fun sendClickBatch(
        userId: String,
        countryCode: String,
        batchClicks: Int,
        durationSeconds: Int = 5,
        onSuccess: () -> Unit
    ) {
        if (batchClicks <= 0) return

        val now = System.currentTimeMillis()
        val signature = AntiCheat.signBatch(
            userId = userId,
            timestamp = now,
            clicks = batchClicks
        )

        val json = JSONObject().apply {
            put("userId", userId)
            put("countryCode", countryCode)
            put("batchClicks", batchClicks)
            put("clientTimestamp", now)
            put("durationSeconds", durationSeconds)
            put("signature", signature)
        }

        val ws = webSocket
        if (ws != null && _serverState.value.isConnected) {
            val sent = ws.send(json.toString())
            if (sent) {
                _serverState.update {
                    it.copy(
                        isSyncing = true,
                        lastSyncTimestamp = now
                    )
                }
                onSuccess()
            }
        } else {
            // Offline local mode: increment local simulation total and execute success callback
            _serverState.update { current ->
                current.copy(
                    globalClicks = current.globalClicks + batchClicks,
                    lastSyncTimestamp = now
                )
            }
            onSuccess()
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")

            when (type) {
                "INIT_STATE", "TICK_UPDATE" -> {
                    val globalClicks = json.optLong("globalClicks", _serverState.value.globalClicks)
                    val onlineCount = json.optInt("onlineCount", _serverState.value.onlineCount)

                    val topCountryObj = json.optJSONObject("topCountry")
                    val topCountry = topCountryObj?.optString("identifier", "TR") ?: "TR"

                    val topCountriesArray = json.optJSONArray("topCountries")
                    val parsedCountries = parseLeaderboard(topCountriesArray)

                    val topUsersArray = json.optJSONArray("topUsers")
                    val parsedUsers = parseLeaderboard(topUsersArray)

                    _serverState.update {
                        it.copy(
                            globalClicks = globalClicks,
                            onlineCount = if (onlineCount > 0) onlineCount else it.onlineCount,
                            topCountry = topCountry,
                            topCountries = if (parsedCountries.isNotEmpty()) parsedCountries else it.topCountries,
                            topUsers = if (parsedUsers.isNotEmpty()) parsedUsers else it.topUsers,
                            isSyncing = false,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    }
                }
                "BATCH_ACK" -> {
                    _serverState.update {
                        it.copy(
                            isSyncing = false,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    }
                }
                "ERROR" -> {
                    val msg = json.optString("message", "Bilinmeyen hata")
                    Log.w(TAG, "Server error: $msg")
                    _serverState.update { it.copy(isSyncing = false) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server message: ${e.message}")
        }
    }

    private fun parseLeaderboard(array: JSONArray?): List<LeaderboardEntry> {
        if (array == null || array.length() == 0) return emptyList()
        val list = mutableListOf<LeaderboardEntry>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val rank = obj.optInt("rank", i + 1)
            val identifier = obj.optString("identifier", obj.optString("userId", obj.optString("country", "")))
            val clicks = obj.optLong("clicks", 0L)
            list.add(LeaderboardEntry(rank = rank, identifier = identifier, clicks = clicks, countryCode = identifier))
        }
        return list
    }

    private fun startPeriodicSimulationFallback() {
        simulationJob?.cancel()
        simulationJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                // If not connected to real backend, simulate subtle real-world global traffic
                if (!_serverState.value.isConnected) {
                    val randomAdd = Random.nextLong(15, 65)
                    _serverState.update { current ->
                        val updatedCountries = current.topCountries.mapIndexed { idx, item ->
                            if (idx == 0) item.copy(clicks = item.clicks + (randomAdd * 0.45).toLong())
                            else item.copy(clicks = item.clicks + (randomAdd * (0.5 / (idx + 1))).toLong())
                        }
                        current.copy(
                            globalClicks = current.globalClicks + randomAdd,
                            topCountries = updatedCountries
                        )
                    }
                }
            }
        }
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Normal disconnect")
            webSocket = null
        } catch (e: Exception) {
            // ignore
        }
        reconnectJob?.cancel()
    }
}
