package jp.ne.sakura.miyadai.roboconRemote

import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.Switch
import androidx.activity.ComponentActivity
import com.longdo.mjpegviewer.MjpegView
import okio.ByteString.Companion.toByteString
import java.util.Timer
import java.util.TimerTask


class MainActivity : ComponentActivity() {
    var ESPisconnect : Boolean = false
    lateinit var ESPWebSocketClient : ESPWebSocketClient
    lateinit var viewer : MjpegView
    lateinit var joyStickSurfaceView: JoyStickSurfaceView
    lateinit var horizontalStickSurfaceview: HorizontalStickSurfaceview
    lateinit var clawlerSwitch2 : Switch
    lateinit var speedseekBar: SeekBar
    private val STREAM_URL = "http://192.168.0.20:81/stream"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ESPWebSocketClient = ESPWebSocketClient(this)

        val timer = Timer()


        joyStickSurfaceView = findViewById(R.id.JoySticksurfaceView)
        horizontalStickSurfaceview = findViewById(R.id.horizontalStickSurfaceview)
        clawlerSwitch2 = findViewById(R.id.switch_clawler2)

        speedseekBar = findViewById(R.id.speed_changer)
        viewer = findViewById(R.id.mjpeg_view)


        speedseekBar.min = 20
        speedseekBar.max = 120
        speedseekBar.progress = 85

        timer.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    if (ESPisconnect) {
                        val speed = speedseekBar.progress
                        var bytes = ByteArray(0)
                        bytes += (joyStickSurfaceView.getPosX * speed).toByteArray()
                        bytes += (joyStickSurfaceView.getPosY * speed * -1).toByteArray()
                        bytes += (horizontalStickSurfaceview.sendX * speed * -1).toByteArray()
                        bytes += (speed and 0xff).toByte()
                        bytes += if (clawlerSwitch2.isChecked) (1).toByte() else (0).toByte()
                        ESPWebSocketClient.send(bytes.toByteString())
                    }
                }
            }, 100, 25
        )

        /*SwitchSeppuku.setOnCheckedChangeListener { _, isChecked ->
          if(isChecked){
              RPIWebSocketClient.send("crawleron")
          }
        }

        SwitchSeppuku.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                RPIWebSocketClient.send("teston")
            } else {
                RPIWebSocketClient.send("testoff")
            }
        };*/

        viewer.mode = MjpegView.MODE_FIT_WIDTH
        viewer.isAdjustHeight = true
        viewer.supportPinchZoomAndPan = false
        viewer.setUrl(STREAM_URL)

        ESPWebSocketClient.connect()
    }

    override fun onPause() {
        super.onPause()
        Log.d("stop", "stop")
        ESPWebSocketClient.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("stop", "stop")
        ESPWebSocketClient.close()
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("restart", "restart")
        ESPWebSocketClient.connect()
    }

    private fun Float.toByteArray() : ByteArray{
        val bytes = ByteArray(4)
        bytes[0] = (this.toRawBits() and 0xFF).toByte()
        bytes[1] = ((this.toRawBits() ushr 8) and 0xFF).toByte()
        bytes[2] = ((this.toRawBits() ushr 16) and 0xFF).toByte()
        bytes[3] = ((this.toRawBits() ushr 24) and 0xFF).toByte()
        return bytes
    }
}



