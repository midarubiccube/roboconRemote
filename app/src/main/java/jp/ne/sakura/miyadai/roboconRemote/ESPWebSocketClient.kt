package jp.ne.sakura.miyadai.roboconRemote;

import android.view.View
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

class ESPWebSocketClient(activity: MainActivity) : WebSocketListener() {
    private var ws: WebSocket
    private val activity : MainActivity = activity

    init {
            val client = OkHttpClient.Builder().pingInterval(5, TimeUnit.SECONDS).build()

            val request = Request.Builder()
                .url("ws://192.168.0.92:8765")
                .build()

            ws = client.newWebSocket(request, this)
        }

        fun send(message: String) {
            ws.send(message)
        }

        fun send(message: ByteString) {
            ws.send(message)
        }

        fun close() {
            ws.close(1000, null)
            activity.ESPisconnect = false
        }

        fun connect()
        {
            val client = OkHttpClient.Builder().pingInterval(5, TimeUnit.SECONDS).build()

            val request = Request.Builder()
                .url("ws://192.168.0.92:8765")
                .build()

            ws = client.newWebSocket(request, this)
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            println("WebSocket opened successfully")
            activity.ESPisconnect = true
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            println("Received text message: $text")
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            println("Received binary message: ${bytes.hex()}")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
            println("Connection closed: $code $reason")
            
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            println("Connection failed: ${t.localizedMessage}")
            activity.ESPisconnect = false
        }
}