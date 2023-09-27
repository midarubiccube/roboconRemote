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


        timer.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    if (isconnect) {
                        webSocketClient.send(MakeSendData(joyStickSurfaceView.getPosX * speed, joyStickSurfaceView.getPosY * speed, horizontalStickSurfaceview.sendX * speed).toByteString())
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

    private fun MakeSendData(posX : Float, posY : Float, posR : Float) : ByteArray {
        val distance : Float =
            sqrt(posX.toDouble().pow(2.0) + posY.toDouble().pow(2.0))
                .toFloat()
        val Xa = abs(posX)
        val Ya = abs(posY)


        val position : Array<Float> = arrayOf(posX, posY, posR)

        if (Xa != 0f && Ya != 0f) {
            if (Xa < Ya) {
                position[0] = position[0] * distance / Ya
                position[1] = position[1] * distance / Ya
            } else if (Xa > Ya) {
                position[0] = position[0] * distance / Xa
                position[1] = position[1] * distance / Xa
            } else if (Xa == Ya) {
                position[0] = position[0] * distance / Xa
                position[1] = position[1] * distance / Ya
            }
        }


        var pwm : Array<Int> = emptyArray();

        pwm += (position[1] + position[0] + position[2]).toInt()
        pwm += (position[1] - position[0] - position[2]).toInt()
        pwm += (position[1] - position[0] + position[2]).toInt()
        pwm += (position[1] + position[0] - position[2]).toInt()
        pwm += if (Switch.isChecked) (position[1] + position[2]).toInt() else 0
        pwm += if (Switch.isChecked) (position[1] - position[2]).toInt() else 0

        val bytes = ByteArray(12)
        val max_pwm = pwm.max()
        for (i in 0..5){
            bytes[i*2] =  if (speed - 1 < max_pwm) ((speed * abs(pwm[i]) / max_pwm )and 0xff).toByte() else (abs(pwm[i]) and 0xff).toByte()
            bytes[i*2+1] =  if (pwm[i] < 0) 1  else 0
        }

        return bytes
    }
}



