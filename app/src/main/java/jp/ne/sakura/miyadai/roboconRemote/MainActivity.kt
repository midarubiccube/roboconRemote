package jp.ne.sakura.miyadai.roboconRemote

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioGroup
import android.widget.Switch
import androidx.activity.ComponentActivity
import com.longdo.mjpegviewer.MjpegView
import okio.ByteString.Companion.toByteString
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


class MainActivity : ComponentActivity() {
    var isconnect : Boolean = false
    private var speed : Int = 230
    lateinit var webSocketClient : WebSocketClient
    lateinit var viewer : MjpegView
    lateinit var joyStickSurfaceView: JoyStickSurfaceView
    lateinit var horizontalStickSurfaceview: HorizontalStickSurfaceview
    lateinit var verticalSurfaceview: VerticalSurfaceview
    lateinit var Switch : Switch
    private val STREAM_URL = "http://192.168.0.20:8000/?action=stream"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webSocketClient = WebSocketClient(this, applicationContext)
        val timer = Timer()


        joyStickSurfaceView = findViewById(R.id.JoySticksurfaceView)
        Switch = findViewById(R.id.switch1)
        val select_button = findViewById<RadioGroup>(R.id.speed_select)
        viewer = findViewById(R.id.mjpeg_view)
        horizontalStickSurfaceview = findViewById(R.id.horizontalStickSurfaceview)
        verticalSurfaceview = findViewById(R.id.verticalSurfaceview)


        timer.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    if (isconnect) {
                        webSocketClient.send(MakeSendData().toByteString())
                    }
                }
            }, 100, 10
        )

        select_button.setOnCheckedChangeListener { _, checkedId: Int ->
            when (checkedId) {
                R.id.radio_button_low -> {
                    speed = 100
                }
                R.id.radio_button_high -> {
                    speed = 230
                }
                else -> throw IllegalArgumentException("not supported")
            }
        }

        viewer.mode = MjpegView.MODE_FIT_WIDTH
        viewer.isAdjustHeight = true
        viewer.supportPinchZoomAndPan = false
        viewer.setUrl(STREAM_URL)

        webSocketClient.send("Hello from Android")

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

    private fun MakeSendData() : ByteArray {
        var bytes = ByteArray(0)
        bytes += (joyStickSurfaceView.getPosX * speed).makeByteArray()
        bytes += (joyStickSurfaceView.getPosY * speed).makeByteArray()
        bytes += (horizontalStickSurfaceview.sendX * speed).makeByteArray()
        bytes += (verticalSurfaceview.sendY * 128).makeByteArray()
        return bytes
    }
    private fun Float.makeByteArray() : ByteArray{
        val bytes = ByteArray(4)
        bytes[0] = (this.toRawBits() and 0xFF).toByte()
        bytes[1] = ((this.toRawBits() ushr 8) and 0xFF).toByte()
        bytes[2] = ((this.toRawBits() ushr 16) and 0xFF).toByte()
        bytes[3] = ((this.toRawBits() ushr 24) and 0xFF).toByte()
        return bytes
    }

}



