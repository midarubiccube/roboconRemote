package jp.ne.sakura.miyadai.roboconRemote

import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.widget.SeekBar
import android.widget.Switch
import com.example.ros2_android_test_app.ROSActivity
import com.longdo.mjpegviewer.MjpegView
import geometry_msgs.msg.Twist
import geometry_msgs.msg.Vector3
import org.ros2.rcljava.node.BaseComposableNode
import org.ros2.rcljava.publisher.Publisher
import std_msgs.msg.UInt16
import java.util.Timer
import java.util.TimerTask

class MainActivity : ROSActivity() {
    lateinit var viewer : MjpegView
    lateinit var Node : BaseComposableNode

    lateinit var JoyStickpublisher: Publisher<Twist>
    lateinit var Powerpublisher: Publisher<UInt16>
    lateinit var Speedpublisher: Publisher<UInt16>

    lateinit var joyStickSurfaceView: JoyStickSurfaceView
    lateinit var horizontalStickSurfaceview: HorizontalStickSurfaceview
    lateinit var verticalSurfaceview: VerticalSurfaceview
    lateinit var Switch : Switch
    lateinit var speedseekBar: SeekBar
    var AXIS_X : Float = 0.0f
    var AXIS_Y : Float = 0.0f
    var AXIS_Z : Float = 0.0f
    var AXIS_RTRIGGER : Float = 0.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        Node = BaseComposableNode("android_controller")//ノード名を設定

        JoyStickpublisher = Node.node.createPublisher(
            Twist::class.java, "/turtle1/cmd_vel" //Publisherを作成
        )

        Powerpublisher = Node.node.createPublisher(
            UInt16::class.java, "/turtle1/poweron" //Publisherを作成
        )

        Speedpublisher = Node.node.createPublisher(
            UInt16::class.java, "/turtle1/speed" //Publisherを作成
        )

        joyStickSurfaceView = findViewById(R.id.JoySticksurfaceView)
        horizontalStickSurfaceview = findViewById(R.id.horizontalStickSurfaceview)
        verticalSurfaceview = findViewById(R.id.verticalSurfaceview)

        Switch = findViewById(R.id.switch_seppuku)

        speedseekBar = findViewById(R.id.speed_changer)

        speedseekBar.min = 20
        speedseekBar.max = 120
        speedseekBar.progress = 85

        executor.addNode(Node)

        timer = Timer()

        Switch.setOnCheckedChangeListener { buttonView, isChecked ->
            val msg = UInt16()
            if (isChecked) {
                msg.data = 1
                Powerpublisher.publish(msg)
            } else {
                msg.data = 0
                Powerpublisher.publish(msg)
            }
        }

        timer.schedule(
            object : TimerTask() {
                override fun run() {
                    val speed = UInt16()
                    speed.data = speedseekBar.progress.toShort()
                    Speedpublisher.publish(speed)

                    val msg = Twist()
                    val linear = Vector3()
                    val angular = Vector3()

                    linear.x = joyStickSurfaceView.getPosX.toDouble() * -1
                    linear.y = joyStickSurfaceView.getPosY.toDouble() * -1
                    angular.x = horizontalStickSurfaceview.getX.toDouble()
                    angular.y = verticalSurfaceview.sendY.toDouble()

                    msg.angular = angular
                    msg.linear = linear
                    JoyStickpublisher.publish(msg);
                }
            }, 100, 10
        )
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {

        // Check that the event came from a game controller
        return if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
            && event.action == MotionEvent.ACTION_MOVE) {

            // Process the movements starting from the
            // earliest historical position in the batch
            (0 until event.historySize).forEach { i ->
                // Process the event at historical position i
                processJoystickInput(event, i)
            }
            processJoystickInput(event, -1)
            true
        } else {
            super.onGenericMotionEvent(event)
        }
    }

    private fun getCenteredAxis(
        event: MotionEvent,
        device: InputDevice,
        axis: Int,
        historyPos: Int
    ): Float {
        val range: InputDevice.MotionRange? = device.getMotionRange(axis, event.source)

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        range?.apply {
            val value: Float = if (historyPos < 0) {
                event.getAxisValue(axis)
            } else {
                event.getHistoricalAxisValue(axis, historyPos)
            }

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            if (Math.abs(value) > flat) {
                return value
            }
        }
        return 0f
    }
    private fun processJoystickInput(event: MotionEvent, historyPos: Int) {
 
        val inputDevice = event.device
        AXIS_X = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_X, historyPos)
        AXIS_Y = getCenteredAxis(event,  inputDevice, MotionEvent.AXIS_Y, historyPos)
        AXIS_Z = getCenteredAxis(event,  inputDevice, MotionEvent.AXIS_Z, historyPos)
        AXIS_RTRIGGER = getCenteredAxis(event,  inputDevice, MotionEvent.AXIS_RTRIGGER, historyPos)

        Log.d("test", AXIS_RTRIGGER.toString())
        joyStickSurfaceView.setPOS(AXIS_X, AXIS_Y)
        horizontalStickSurfaceview.setx(AXIS_Z)
        verticalSurfaceview.sety(AXIS_RTRIGGER)
    }

    override fun onPause() {
        super.onPause()
        Log.d("stop", "stop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("stop", "stop")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("restart", "restart")
    }

    fun getGameControllerIds(): List<Int> {
        val gameControllerDeviceIds = mutableListOf<Int>()
        val deviceIds = InputDevice.getDeviceIds()
        deviceIds.forEach { deviceId ->
            InputDevice.getDevice(deviceId).apply {

                // Verify that the device has gamepad buttons, control sticks, or both.
                if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
                    || sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
                    // This device is a game controller. Store its device ID.
                    gameControllerDeviceIds
                        .takeIf { !it.contains(deviceId) }
                        ?.add(deviceId)
                }
            }
        }
        return gameControllerDeviceIds
    }
}


