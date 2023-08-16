package jp.ne.sakura.miyadai.roboconRemote

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import java.lang.String
import kotlin.Float

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val JoyStick = findViewById<JoyStickSurfaceView>(R.id.JoySticksurfaceView)

        JoyStick.setOnJoyStickMoveListener(object : JoyStickSurfaceView.OnJoystickMoveListener {
            override fun onValueChanged(angle: Float, power: Float, state: JoyStickSurfaceView.JoyStick?) {
            }
        }, JoyStickSurfaceView.LOOP_INTERVAL_SLOW, JoyStickSurfaceView.LOOP_INTERVAL_FAST)

        JoyStick.setOnLongPushListener(object : JoyStickSurfaceView.OnLongPushListener {
            override fun onLongPush() {
                Log.d("MainEvent", "long pushed")
            }
        })

        JoyStick.setOnChangeStateListener(object : JoyStickSurfaceView.OnChangeStateListener {
            override fun onChangeState(
                next: JoyStickSurfaceView.JoyStick?,
                previous: JoyStickSurfaceView.JoyStick?
            ) {
            }
        })

    }
}