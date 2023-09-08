package jp.ne.sakura.miyadai.roboconRemote

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import com.longdo.mjpegviewer.MjpegView
import okio.ByteString.Companion.toByteString
import kotlin.Float

class MainActivity : ComponentActivity() {
    var isconnect : Boolean = false

    val STREAM_URL = "http://192.168.0.20:8000/?action=stream"

    override fun onCreate(savedInstanceState: Bundle?) {
        val webSocketClient = WebSocketClient(this, applicationContext)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewer = findViewById<View>(R.id.mjpeg_view) as MjpegView
        viewer.mode = MjpegView.MODE_FIT_WIDTH
        viewer.isAdjustHeight = true
        viewer.supportPinchZoomAndPan = false
        viewer.setUrl(STREAM_URL)
        viewer.startStream()

        webSocketClient.send("Hello from Android")

        val joyStickSurfaceView = findViewById<JoyStickSurfaceView>(R.id.JoySticksurfaceView)

        joyStickSurfaceView.setOnJoyStickMoveListener(object : JoyStickSurfaceView.OnJoystickMoveListener {
            override fun onValueChanged(angle: Float, power: Float, state: JoyStickSurfaceView.JoyStick?) {
                if (isconnect){
                    webSocketClient.send(angle.toRawBits().toByteArray().toByteString())
                    webSocketClient.send(power.toRawBits().toByteArray().toByteString())
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
}


fun Int.toByteArray(): ByteArray {
    val bytes = ByteArray(4)
    bytes[0] = (this and 0xFF).toByte()
    bytes[1] = ((this ushr 8) and 0xFF).toByte()
    bytes[2] = ((this ushr 16) and 0xFF).toByte()
    bytes[3] = ((this ushr 24) and 0xFF).toByte()
    return bytes
}

