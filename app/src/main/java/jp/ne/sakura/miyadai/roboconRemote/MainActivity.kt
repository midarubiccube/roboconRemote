package jp.ne.sakura.miyadai.roboconRemote

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import com.longdo.mjpegviewer.MjpegView
import okio.ByteString.Companion.toByteString
import okhttp3.WebSocket
import kotlin.Float

class MainActivity : ComponentActivity() {
    var isconnect : Boolean = false
    lateinit var webSocketClient : WebSocketClient
    lateinit var viewer : MjpegView
    val STREAM_URL = "http://192.168.0.20:8000/?action=stream"

    override fun onCreate(savedInstanceState: Bundle?) {
        webSocketClient = WebSocketClient(this, applicationContext)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewer = findViewById<View>(R.id.mjpeg_view) as MjpegView
        viewer.mode = MjpegView.MODE_FIT_WIDTH
        viewer.isAdjustHeight = true
        viewer.supportPinchZoomAndPan = false
        viewer.setUrl(STREAM_URL)

        webSocketClient.send("Hello from Android")

        val joyStickSurfaceView = findViewById<JoyStickSurfaceView>(R.id.JoySticksurfaceView)

        joyStickSurfaceView.setOnJoyStickMoveListener(object : JoyStickSurfaceView.OnJoystickMoveListener {
            override fun onValueChanged(angle: Float, power: Float, state: JoyStickSurfaceView.JoyStick?) {
                if (isconnect){
                    webSocketClient.send(annexation(angle.toRawBits(), power.toRawBits()).toByteString())
                }
            }
        }, JoyStickSurfaceView.LOOP_INTERVAL_SLOW, JoyStickSurfaceView.LOOP_INTERVAL_FAST)

        joyStickSurfaceView.setOnLongPushListener(object : JoyStickSurfaceView.OnLongPushListener {
            override fun onLongPush() {
                Log.d("MainEvent", "long pushed")
            }
        })

        joyStickSurfaceView.setOnChangeStateListener(object : JoyStickSurfaceView.OnChangeStateListener {
            override fun onChangeState(
                next: JoyStickSurfaceView.JoyStick?,
                previous: JoyStickSurfaceView.JoyStick?
            ) {
            }
        })

    }

    override fun onPause() {
        super.onPause()
        Log.d("stop", "stop")
        webSocketClient.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("stop", "stop")
        webSocketClient.close()
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("restart", "restart")
        webSocketClient.connect()
    }
    fun annexation(x : Int, y : Int): ByteArray {
        val bytes = ByteArray(8)
        bytes[0] = (x and 0xFF).toByte()
        bytes[1] = ((x ushr 8) and 0xFF).toByte()
        bytes[2] = ((x ushr 16) and 0xFF).toByte()
        bytes[3] = ((x ushr 24) and 0xFF).toByte()
        bytes[4] = (y and 0xFF).toByte()
        bytes[5] = ((y ushr 8) and 0xFF).toByte()
        bytes[6] = ((y ushr 16) and 0xFF).toByte()
        bytes[7] = ((y ushr 24) and 0xFF).toByte()
        return bytes
    }
}



