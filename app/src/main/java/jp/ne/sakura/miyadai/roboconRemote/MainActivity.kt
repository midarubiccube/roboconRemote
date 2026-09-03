package jp.ne.sakura.miyadai.roboconRemote

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.system.Os
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import geometry_msgs.msg.Twist
import geometry_msgs.msg.Vector3
import ros2can.msg.PWRManagerTX
import org.ros2.rcljava.RCLJava
import org.ros2.rcljava.executors.Executor
import org.ros2.rcljava.executors.SingleThreadedExecutor
import org.ros2.rcljava.node.BaseComposableNode
import org.ros2.rcljava.publisher.Publisher
import org.ros2.rcljava.subscription.Subscription
import std_msgs.msg.Float32
import std_msgs.msg.String
import std_msgs.msg.UInt16
import java.util.Timer
import java.util.TimerTask


class MainActivity : ComponentActivity() {
    lateinit var executor: Executor
    lateinit var send_timer: Timer
    lateinit var send_power : Timer
    lateinit var timer : Timer
    lateinit var handler: Handler

    lateinit var Node : BaseComposableNode

    lateinit var JoyStickpublisher: Publisher<Twist>
    lateinit var Powerpublisher: Publisher<PWRManagerTX>

    lateinit var joyStickSurfaceView: JoyStickSurfaceView
    lateinit var horizontalStickSurfaceview: HorizontalStickSurfaceview
    lateinit var verticalSurfaceview: VerticalSurfaceview
    lateinit var verticalSurfaceview2: VerticalSurfaceview

    lateinit var Switch : Switch
    lateinit var Switch2 : Switch
    lateinit var speedseekBar: SeekBar
    lateinit var text : TextView

    var R1Status = false
    var L1Status = false


    private val SPINNER_PERIOD_MS : Long = 200
    private val SPINNER_DELAY : Long  = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        joyStickSurfaceView = findViewById(R.id.JoySticksurfaceView)
        horizontalStickSurfaceview = findViewById(R.id.horizontalStickSurfaceview)
        verticalSurfaceview = findViewById(R.id.verticalSurfaceview)
        verticalSurfaceview2 = findViewById(R.id.verticalSurfaceview2)

        Switch = findViewById(R.id.switch_seppuku)
        Switch2 = findViewById(R.id.switch_seppuku2)

        speedseekBar = findViewById(R.id.speed_changer)

        speedseekBar.min = 50
        speedseekBar.max = 100
        speedseekBar.progress = 75

        send_timer = Timer()
        this.handler = Handler(mainLooper)
        this.executor = this.createExecutor()

        initROS()

        Switch.setOnCheckedChangeListener { buttonView, isChecked ->
            val msg = PWRManagerTX()
            if (isChecked) {
                msg.priority = 0
                msg.powerstatus = true
                Powerpublisher.publish(msg)
            } else {
                msg.powerstatus = false
                Powerpublisher.publish(msg)
            }
        }

        timer = Timer()
        timer.schedule(
            object : TimerTask() {
                override fun run() {
                    val runnable = Runnable { executor.spinSome() }
                    handler.post(runnable)
                }
            }, SPINNER_DELAY, SPINNER_PERIOD_MS)
    }

    private fun initROS(){
        Os.setenv("ROS_DOMAIN_ID", 0.toString(), true)
        this.handler = Handler(mainLooper)
        RCLJava.rclJavaInit()
        this.executor = this.createExecutor()

        Node = BaseComposableNode("android_controller")//ノード名を設定

        JoyStickpublisher = Node.node.createPublisher(
            Twist::class.java, "/turtle1/cmd_vel" //Publisherを作成
        )

        Powerpublisher = Node.node.createPublisher(
            PWRManagerTX::class.java, "PWRManager_TX" //Publisherを作成
        )

        executor.addNode(Node)
        setSendTimer()
    }

    private fun setSendTimer(){
        send_timer = Timer()
        send_timer.schedule(
            object : TimerTask() {
                override fun run() {
                    val msg = Twist()
                    val linear = Vector3()
                    val angular = Vector3()

                    linear.x = joyStickSurfaceView.getAngle()
                    linear.y = joyStickSurfaceView.getDistance.toDouble() * speedseekBar.progress.toDouble()
                    linear.z = horizontalStickSurfaceview.getX.toDouble() *speedseekBar.progress.toDouble()
                    angular.x = verticalSurfaceview2.getY.toDouble()
                    angular.y = verticalSurfaceview.getY.toDouble()
                    if (Switch.isChecked) {
                        angular.z = 1.0
                    } else{
                        angular.z = 0.0
                    }

                    msg.angular = angular
                    msg.linear = linear
                    JoyStickpublisher.publish(msg);
                }
            }, 100, 100
        )
        send_power = Timer()
        send_power.schedule(
            object : TimerTask() {
                override fun run() {
                    val msg = PWRManagerTX()
                    if (Switch.isChecked)
                    {
                        msg.priority = 0
                        msg.powerstatus = true
                    } else{
                        msg.priority = 0
                        msg.powerstatus = false
                    }
                    Powerpublisher.publish(msg)
                }
            }, 100, 500
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
        val AXIS_X = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_X, historyPos)
        val AXIS_Y = getCenteredAxis(event,  inputDevice, MotionEvent.AXIS_Y, historyPos)
        val AXIS_Z = getCenteredAxis(event,  inputDevice, MotionEvent.AXIS_Z, historyPos)
        var AXIS_RTRIGGER = getCenteredAxis(event,  inputDevice, MotionEvent.AXIS_RTRIGGER, historyPos)
        val AXIS_LTRIGGER = getCenteredAxis(event,  inputDevice, MotionEvent.AXIS_LTRIGGER, historyPos)
        val speed = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_HAT_X, historyPos)

        joyStickSurfaceView.setPOS(AXIS_X, AXIS_Y)
        horizontalStickSurfaceview.setx(AXIS_Z)
        verticalSurfaceview.sety(if (R1Status) AXIS_RTRIGGER * -1.0f else AXIS_RTRIGGER)
        verticalSurfaceview2.sety(if (L1Status) AXIS_LTRIGGER * -1.0f else AXIS_LTRIGGER)
        speedseekBar.progress += speed.toInt()*5

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var handled = true
        if (event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) {
            if (event.repeatCount == 0) {
                when (keyCode) {
                    // Handle gamepad and D-pad button presses to navigate the ship
                    KeyEvent.KEYCODE_BUTTON_R1 -> R1Status = false
                    KeyEvent.KEYCODE_BUTTON_L1 -> L1Status = false
                    KeyEvent.KEYCODE_DPAD_LEFT -> Log.d("tezt", "tesr")

                    else -> {
                        handled = false
                    }
                }
            }
            if (handled) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        var handled = true
        if (event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) {
            if (event.repeatCount == 0) {
                when (keyCode) {
                    // Handle gamepad and D-pad button presses to navigate the ship
                    KeyEvent.KEYCODE_BUTTON_R1 -> R1Status = true
                    KeyEvent.KEYCODE_BUTTON_L1 -> L1Status = true
                    else -> {
                        handled = false
                    }
                }
            }
            return handled
        }
        return super.onKeyUp(keyCode, event)
    }
    private fun isFireKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A

    override fun onResume() {
        super.onResume()
        timer = Timer()
        timer.schedule(
            object : TimerTask() {
                override fun run() {
                    val runnable = Runnable { executor.spinSome() }
                    handler.post(runnable)
                }
            }, SPINNER_DELAY, SPINNER_PERIOD_MS)
    }

    override fun onPause() {
        super.onPause()

        val msg = PWRManagerTX()
        msg.priority = 0
        msg.powerstatus = false
        Powerpublisher.publish(msg)

        send_timer.cancel()
        timer.cancel()
        send_power.cancel()
        Log.d("stop", "stop")
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
        send_timer.cancel()
        send_power.cancel()
        Log.d("stop", "stop")
    }

    override fun onRestart() {
        super.onRestart()
        setSendTimer()
        Log.d("restart", "restart")
    }

    private fun createExecutor(): Executor {
        return SingleThreadedExecutor()
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


