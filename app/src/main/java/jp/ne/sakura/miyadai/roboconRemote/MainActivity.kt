package jp.ne.sakura.miyadai.roboconRemote

import android.os.Bundle
import android.os.Handler
import android.system.Os
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import geometry_msgs.msg.Twist
import geometry_msgs.msg.Vector3
import org.ros2.rcljava.RCLJava
import org.ros2.rcljava.executors.Executor
import org.ros2.rcljava.executors.SingleThreadedExecutor
import org.ros2.rcljava.node.BaseComposableNode
import org.ros2.rcljava.publisher.Publisher
import org.ros2.rcljava.subscription.Subscription
import ros2can.msg.BLDCRX
import ros2can.msg.BLDCTX
import ros2can.msg.PWRManagerRX
import ros2can.msg.PWRManagerTX
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

    lateinit var JoyStickpublisher : Publisher<Twist>

    lateinit var Powerpublisher : Publisher<PWRManagerTX>
    lateinit var PowerSubscriber : Subscription<PWRManagerRX>

    lateinit var BLDCpublisher : Publisher<BLDCTX>
    lateinit var BLDCSubscriber : Subscription<BLDCRX>

    lateinit var joyStickSurfaceView: JoyStickSurfaceView
    lateinit var horizontalStickSurfaceview: HorizontalStickSurfaceview

    lateinit var Switch : Switch
    lateinit var limitcurent : EditText
    lateinit var speedseekBar: SeekBar

    lateinit var battery1_vol : TextView
    lateinit var battery2_vol : TextView
    lateinit var current_text : TextView

    var R1Status = false
    var L1Status = false
    private var limit_current = 20.0f

    private val SPINNER_PERIOD_MS : Long = 200
    private val SPINNER_DELAY : Long  = 0

    var AXIS = FloatArray(8)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        joyStickSurfaceView = findViewById(R.id.JoySticksurfaceView)
        horizontalStickSurfaceview = findViewById(R.id.horizontalStickSurfaceview)

        limitcurent = findViewById(R.id.max_current)
        Switch = findViewById(R.id.switch_power)

        battery1_vol = findViewById(R.id.battery1_vol)
        battery2_vol = findViewById(R.id.battery2_vol)
        current_text = findViewById(R.id.current)

        speedseekBar = findViewById(R.id.speed_changer)

        speedseekBar.min = 10
        speedseekBar.max = 300
        speedseekBar.progress = 200

        send_timer = Timer()
        this.handler = Handler(mainLooper)
        this.executor = this.createExecutor()

        initROS()

        Switch.setOnCheckedChangeListener { buttonView, isChecked ->
            val msg = PWRManagerTX()
            msg.priority = 0
            msg.currentLimit = limit_current
            if (isChecked) {
                msg.priority = 0
                msg.powerstatus = true
                Powerpublisher.publish(msg)
            } else {
                msg.powerstatus = false
                Powerpublisher.publish(msg)
            }
        }

        limitcurent.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                limit_current = limitcurent.text.toString().toInt().toFloat()
                false
            } else {
                false // 次の処理にイベントを渡す場合はfalseを返す
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
            Twist::class.java, "/asi/cmd_vel" //Publisherを作成
        )

        Powerpublisher = Node.node.createPublisher(
            PWRManagerTX::class.java, "/PWRManager/TX" //Publisherを作成
        )

        PowerSubscriber = Node.node.createSubscription(
            PWRManagerRX::class.java,
            "PWRManager/RX",
            { msg -> PWR_RX(msg) }
        )

        BLDCpublisher = Node.node.createPublisher(
            BLDCTX::class.java, "/BLDC/TX"
        )

        BLDCSubscriber = Node.node.createSubscription(
            BLDCRX::class.java,
            "/BLDC/RX",
            { msg -> BLDC_RX(msg) }
        )

        executor.addNode(Node)
        setSendTimer()
    }

    private fun  PWR_RX(msg: PWRManagerRX)
    {
        battery1_vol.text = "%.2f".format(msg.battery1Voltage)
        battery2_vol.text = "%.2f".format(msg.battery2Voltage)
        current_text.text = "%.2f".format(msg.current)
    }

    private fun BLDC_RX(msg: BLDCRX)
    {

    }

    private fun setSendTimer(){
        send_timer = Timer()
        send_timer.schedule(
            object : TimerTask() {
                override fun run() {6
                    val speed = speedseekBar.progress / 100.0
                    val msg = Twist()
                    val linear = Vector3()
                    val angular = Vector3()

                    linear.x = (AXIS[0] + joyStickSurfaceView.getPosX) * speed
                    linear.y = (AXIS[1] + joyStickSurfaceView.getPosY) * speed
                    linear.z = (AXIS[2] + horizontalStickSurfaceview.getX) * speed
                    angular.x = AXIS[3].toDouble()
                    angular.y = AXIS[4].toDouble()
                    angular.z = AXIS[5].toDouble()

                    msg.angular = angular
                    msg.linear = linear
                    JoyStickpublisher.publish(msg);

                    val bldc = BLDCTX()
                    bldc.boardNum = 4;
                    bldc.encoderResolution = 4096
                    bldc.gearRatio = 19.2f
                    bldc.monitorFlag = true
                    bldc.monitorFreq = 100

                    bldc.rpsTarget = AXIS[7] * 50.0f
                    BLDCpublisher.publish(bldc)
                }
            }, 100, 50
        )
        send_power = Timer()
        send_power.schedule(
            object : TimerTask() {
                override fun run() {
                    val msg = PWRManagerTX()
                    msg.currentLimit = limit_current
                    msg.batteryLimit = 11.1f
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
        val speed = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_HAT_X, historyPos)

        AXIS[0] = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_X, historyPos)
        AXIS[1] = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Y, historyPos)
        AXIS[2] = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Z, historyPos)
        AXIS[3] = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_RZ, historyPos)
        AXIS[4] = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_RTRIGGER, historyPos)
        AXIS[5] = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_LTRIGGER, historyPos)
        AXIS[6] = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_HAT_X, historyPos)
        AXIS[7] = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_HAT_X, historyPos)

        speedseekBar.progress += speed.toInt()*5
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var handled = true
        if (event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) {
            when (keyCode) {
                // Handle gamepad and D-pad button presses to navigate the ship
                KeyEvent.KEYCODE_BUTTON_R1 -> speedseekBar.progress += 20
                KeyEvent.KEYCODE_BUTTON_L1 -> speedseekBar.progress -= 20
                else -> {
                    handled = false
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
                    KeyEvent.KEYCODE_BUTTON_X -> {
                        val msg = PWRManagerTX()
                        msg.currentLimit = limit_current
                        msg.priority = 0
                        if (Switch.isChecked)
                        {
                            Switch.isChecked = false
                            msg.powerstatus = false
                        } else {
                            Switch.isChecked = true
                            msg.powerstatus = true
                        }
                        Powerpublisher.publish(msg)
                    }
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

}


