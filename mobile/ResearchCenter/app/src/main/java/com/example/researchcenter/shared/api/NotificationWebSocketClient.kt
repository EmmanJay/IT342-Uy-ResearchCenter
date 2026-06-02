package com.example.researchcenter.shared.api

import com.example.researchcenter.BuildConfig
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object NotificationWebSocketClient {
    private var webSocket: WebSocket? = null
    private val listeners = mutableListOf<NotificationListener>()
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    interface NotificationListener {
        fun onNotificationReceived(type: String, message: String, data: JSONObject?)
        fun onConnectionStateChanged(connected: Boolean)
    }

    fun addListener(listener: NotificationListener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: NotificationListener) {
        listeners.remove(listener)
    }

    fun connect(token: String) {
        if (webSocket != null) return

        val request = Request.Builder()
            .url("${BuildConfig.WS_URL}?token=$token")
            .addHeader("Bypass-Tunnel-Reminder", "true")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                listeners.forEach { it.onConnectionStateChanged(true) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type", "UNKNOWN")
                    val message = json.optString("message", "")
                    val data = json.optJSONObject("data")
                    listeners.forEach { it.onNotificationReceived(type, message, data) }
                } catch (e: Exception) {}
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listeners.forEach { it.onConnectionStateChanged(false) }
                NotificationWebSocketClient.webSocket = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listeners.forEach { it.onConnectionStateChanged(false) }
                NotificationWebSocketClient.webSocket = null
            }
        })
    }
    
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }
}
