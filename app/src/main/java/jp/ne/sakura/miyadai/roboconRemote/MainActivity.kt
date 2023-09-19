package jp.ne.sakura.miyadai.roboconRemote

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.content.PackageManagerCompat.LOG_TAG
import com.longdo.mjpegviewer.MjpegView
import okio.ByteString.Companion.toByteString
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


class MainActivity : ComponentActivity() {
    var isconnect : Boolean = false
    lateinit var webSocketClient : WebSocketClient
    lateinit var viewer : MjpegView
    lateinit var joyStickSurfaceView: JoyStickSurfaceView
    val STREAM_URL = "http://192.168.0.20:8000/?action=stream"

    override fun onCreate(savedInstanceState: Bundle?) {
        webSocketClient = WebSocketClient(this, applicationContext)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        joyStickSurfaceView = findViewById(R.id.JoySticksurfaceView)

        val timer = Timer()

        timer.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    if (isconnect) {
                        webSocketClient.send(MakeSendData(joyStickSurfaceView.getPosX(), joyStickSurfaceView.getPosY()).toByteString())
                    }
                }
            }, 100, 80
        )

        viewer = findViewById<View>(R.id.mjpeg_view) as MjpegView
        viewer.mode = MjpegView.MODE_FIT_WIDTH
        viewer.isAdjustHeight = true
        viewer.supportPinchZoomAndPan = false
        viewer.setUrl(STREAM_URL)

        webSocketClient.send("Hello from Android")



        joyStickSurfaceView.setOnJoyStickMoveListener(object : JoyStickSurfaceView.OnJoystickMoveListener {
            override fun onValueChanged(angle: Float, power: Float, state: JoyStickSurfaceView.JoyStick?) {
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

    private fun MakeSendData(posX : Float, posY : Float) : ByteArray {
        val distance : Float =
            sqrt(posX.toDouble().pow(2.0) + posY.toDouble().pow(2.0))
                .toFloat()
        val Xa = abs(posX)
        val Ya = abs(posY)

        val position : Array<Float> = arrayOf(posX, posY)

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
        var pwm : Array<Int> = emptyArray();

        pwm += (position[1] + position[0] + 0).toInt()
        pwm += (position[1] - position[0] - 0).toInt()
        pwm += (position[1] - position[0] + 0).toInt()
        pwm += (position[1] + position[0] - 0).toInt()
        pwm +=  (position[1] + 0).toInt()
        pwm += (position[1] + 0).toInt()

        val bytes = ByteArray(12)
        val max_pwm = pwm.max()
        for (i in 0..5){
            bytes[i*2] =  if (249 < max_pwm) ((250 * abs(pwm[i]) / max_pwm )and 0xff).toByte() else (abs(pwm[i]) and 0xff).toByte()
            bytes[i*2+1] =  if (pwm[i] < 0) 1  else 0
        }

        return bytes
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



