package jp.ne.sakura.miyadai.roboconRemote

import android.os.Bundle
import android.os.Handler
import android.system.Os
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Button
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
import ros2can.msg.MotorBoardTX
import ros2can.msg.ServoTX
import java.util.Timer
import java.util.TimerTask


class MainActivity : ComponentActivity() {
    lateinit var executor: Executor
    lateinit var send_timer: Timer
    lateinit var timer : Timer
    lateinit var handler: Handler

    lateinit var Node : BaseComposableNode

    lateinit var JoyStickpublisher: Publisher<Twist>

    lateinit var BLDCTXpublisher : Publisher<BLDCTX>
    lateinit var BLDCRXSubscriber: Subscription<BLDCRX>

    lateinit var MotorTXpublisher : Publisher<MotorBoardTX>

    lateinit var ServoTXpublisher : Publisher<ServoTX>

    lateinit var horizontalStickSurfaceview: HorizontalStickSurfaceview
    lateinit var rollerspeed : SeekBar

    lateinit var rollerSwitch : Switch
    lateinit var brashSwitch : Switch
    lateinit var resetButton: Button

    lateinit var rpm_text : TextView
    lateinit var bldc_rx: TextView


    var R1Status = false
    var L1Status = false

    private val SPINNER_PERIOD_MS : Long = 200
    private val SPINNER_DELAY : Long  = 0

    private var kaiten_position : Short = 0
    private var updown_position : Short = 0
    private var kakudo_position : Short = 0


    var AXIS = FloatArray(8)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        horizontalStickSurfaceview = findViewById(R.id.horizontalStickSurfaceview)
        rpm_text = findViewById(R.id.RPM_text)
        rollerspeed = findViewById(R.id.roller_speed)
        bldc_rx = findViewById(R.id.bldc_rpm)
        rollerSwitch = findViewById(R.id.switch_roller)
        brashSwitch = findViewById(R.id.switch_brush)
        resetButton = findViewById(R.id.resetbutton)
        rollerspeed.min = 3000
        rollerspeed.max = 7000
        rollerspeed.progress = 3000

        resetButton.setOnClickListener(
            {
                kaiten_position = 0
                updown_position = 0
                kakudo_position = 0
            }
        )

        rollerspeed.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    if (rollerSwitch.isChecked) {
                        rpm_text.text = "$p1 RPM"
                    } else {
                        rpm_text.text = "0 RPM"
                    }
                }
                override fun onStartTrackingTouch(p0: SeekBar?) {
                }
                override fun onStopTrackingTouch(p0: SeekBar?) {
                }
            }
        )

        send_timer = Timer()
        this.handler = Handler(mainLooper)
        this.executor = this.createExecutor()

        initROS()

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

        Node = BaseComposableNode("android_controller_roller")//ノード名を設定

        JoyStickpublisher = Node.node.createPublisher(
            Twist::class.java, "/roller/cmd_vel" //Publisherを作成
        )

        BLDCTXpublisher = Node.node.createPublisher(
            BLDCTX::class.java, "BLDC/TX" //Publisherを作成
        )

        BLDCRXSubscriber = Node.node.createSubscription(
            BLDCRX::class.java,
            "/BLDC/RX",
            { msg -> PWR_RX(msg) }
        )

        MotorTXpublisher = Node.node.createPublisher(
            MotorBoardTX::class.java, "/MotorBoard/TX"
        )

        ServoTXpublisher = Node.node.createPublisher(
            ServoTX::class.java, "/Servo/TX"
        )

        executor.addNode(Node)
        setSendTimer()
    }

    private fun PWR_RX(msg : BLDCRX)
    {
        if (msg.boardNum.toInt() == 5)
        {
            bldc_rx.text = "%.2fRPM".format(msg.rps*60.0f)
        }
    }

    private fun setSendTimer(){
        send_timer = Timer()
        send_timer.schedule(
            object : TimerTask() {
                override fun run() {6
                    val msg = Twist()
                    val linear = Vector3()
                    val angular = Vector3()

                    val bldctx = BLDCTX()
                    bldctx.priority = 0
                    bldctx.boardNum = 5
                    bldctx.gearRatio = 1f
                    bldctx.encoderResolution = 4096
                    bldctx.monitorFlag = true
                    bldctx.monitorFreq = 100
                    if (rollerSwitch.isChecked){
                        bldctx.rpsTarget = rollerspeed.progress/60.0f
                    } else {
                        bldctx.rpsTarget = 0.0f
                    }
                    BLDCTXpublisher.publish(bldctx)
                    bldctx.boardNum = 6
                    bldctx.monitorFlag = false
                    bldctx.monitorFreq = 0
                    bldctx.rpsTarget = bldctx.rpsTarget*-1
                    BLDCTXpublisher.publish(bldctx)

                    val motormsg = MotorBoardTX()
                    motormsg.boardNum = 0
                    motormsg.mode[0] = 1
                    motormsg.target[0] = if (brashSwitch.isChecked) 100 else 0
                    MotorTXpublisher.publish(motormsg)

                    val servo = ServoTX()
                    servo.boardNum = 0
                    servo.channnel = 0
                    kaiten_position = (kaiten_position + (AXIS[0] * -200.0).toInt()).toShort()
                    servo.position[0] = kaiten_position
                    servo.time[0] = 0
                    servo.speed[0] = 20

                    updown_position = (updown_position + (AXIS[3] * -200.0).toInt()).toShort()
                    servo.position[1] = updown_position
                    servo.time[1] = 0
                    servo.speed[1 ] = 20

                    if (kakudo_position >=  0) {
                        kakudo_position = (kakudo_position + (AXIS[7] * -10.0).toInt()).toShort()
                    } else {
                        kakudo_position = 0
                    }
                    servo.position[2] = kakudo_position
                    servo.time[2] = 0
                    servo.speed[2] = 20
                    servo.monitorFreq = if (brashSwitch.isChecked) 200 else 0
                    ServoTXpublisher.publish(servo)
                }
            }, 100, 200
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
        AXIS[0] = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_X, historyPos)
        AXIS[1] = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Y, historyPos)
        AXIS[2] = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Z, historyPos)
        AXIS[3] = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_RZ, historyPos)
        AXIS[4] = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_RTRIGGER, historyPos)
        AXIS[5] = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_LTRIGGER, historyPos)
        AXIS[6] = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_HAT_X, historyPos)
        AXIS[7] = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_HAT_Y, historyPos)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var handled = true
        if (event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) {
            when (keyCode) {
                // Handle gamepad and D-pad button presses to navigate the ship
                KeyEvent.KEYCODE_BUTTON_R1 -> rollerspeed.progress += 250
                KeyEvent.KEYCODE_BUTTON_L1 -> rollerspeed.progress -= 250
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
                    KeyEvent.KEYCODE_BUTTON_R1 -> R1Status = true
                    KeyEvent.KEYCODE_BUTTON_L1 -> L1Status = true
                    KeyEvent.KEYCODE_BUTTON_A -> {
                        if (rollerSwitch.isChecked)
                        {
                            rollerSwitch.isChecked = false
                            rpm_text.text = "0 RPM"

                        } else {
                            rollerSwitch.isChecked = true
                            rpm_text.text = "${rollerspeed.progress} RPM"
                        }
                    }

                    KeyEvent.KEYCODE_BUTTON_X -> {
                        if (brashSwitch.isChecked)
                        {
                            brashSwitch.isChecked = false
                        } else {
                            brashSwitch.isChecked = true
                        }
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

        send_timer.cancel()
        timer.cancel()
        Log.d("stop", "stop")
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
        send_timer.cancel()
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


