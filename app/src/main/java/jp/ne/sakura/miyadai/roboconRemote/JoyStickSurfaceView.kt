package jp.ne.sakura.miyadai.roboconRemote

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import kotlin.math.cos
import kotlin.math.sin

/**
 * Created by rild on 2017/04/10.
 */
class JoyStickSurfaceView(context: Context, attrs: AttributeSet?) :
    SurfaceView(context, attrs), SurfaceHolder.Callback {
    private val RESID_STICK_DEFAULT: Int = R.drawable.s_joystick_stick
    private val RESID_BACKGROUND_DEFAULT: Int = R.drawable.s_joystick_base
    private val RESID_SHADOW_DEFAULT: Int = R.drawable.s_joystick_shadow
    private val RESID_SIG_UP_DEFAULT: Int = R.drawable.s_signal_up
    private val RESID_SIG_RIGHT_DEFAULT: Int = R.drawable.s_signal_right
    private val RESID_SIG_DOWN_DEFAULT: Int = R.drawable.s_signal_down
    private val RESID_SIG_LEFT_DEFAULT: Int = R.drawable.s_signal_left
    private val USE_SIG_DEFAULT = true
    private val DENO_RATE_STICK_TALL_TO_SIZE = 25
    private val DENO_RATE_STICK_SIZE_TO_PAD = 2
    private val DENO_RATE_OFFSET_TO_PAD = 3
    private val DENO_RATE_MIN_DISTANCE_TO_PAD = 1
    private val ALPHA_PAD_DEFAULT = 150
    private val ALPHA_STICK_DEFAULT = 180
    private val ALPHA_SIGNAL_DEFAULT = 140
    private var alphaStick = 200
    private var alphaLayout = 200
    var alphaSignal = 200
        private set
    var offset = 0
    private var surfaceHolder: SurfaceHolder? = null

    //    private Context mContext;
    private var params: ViewGroup.LayoutParams? = null
    private var stickTall = 0
    private var stickWidth = 0
    private var stickHeight = 0
    private var positionX = 0
    private var positionY = 0
    var minimumDistance = 100
    private var distance = 0f
    private var angle = 0f
    private val jsEntity // joy stick entity
            : JoyStickEntity
    private var alphaSigPaint: Paint? = null
    private var alphaBacksPaint: Paint? = null
    private var alphaStickPaint: Paint? = null
    private val res: Resources
    private var background: Bitmap? = null
    private var stick: Bitmap? = null
    private var signalUp: Bitmap? = null
    private var signalRight: Bitmap? = null
    private var signalDown: Bitmap? = null
    private var signalLeft: Bitmap? = null
    private var canUseSignal = USE_SIG_DEFAULT
    private var stickState = JoyStick.NONE

    /**
     * ----- variables for event managing below here ---------
     */
    private var onChangeStateListener: OnChangeStateListener? = null

    /**
     * `hasFastLoop == false` method:
     * onJoyStickMoveListener.(float angle, float power, JoyStickSurfaceView.JoyStick state)
     * will loop only with loopInterval
     *
     *
     * `hasFastLoop == true` the method will loop with
     * two different interval.
     *
     * @param loopInterval
     * @param loopFastInterval
     *
     *
     * loop interval depends on
     * @param distance
     * <-- ignore -->
     * minDistance
     * <-- slow interval, weak signal -->
     * midDistance = (params.width / 2) - offset
     * <-- fast interval, strong signal -->
     */
    private val LOOP_INTERVAL_DEFAULT: Long = 800 // original 100 ms
    private var loopInterval = LOOP_INTERVAL_DEFAULT
    private var loopFastInterval = LOOP_INTERVAL_DEFAULT
    private var hasFastLoop = false
    private var onJoyStickMoveListener: OnJoystickMoveListener? = null
    private var threadJoyStickMove: Thread? = null
    private val TIME_LONG_PUSH_EVENT_ACTIVATE = 1_500
    private val handlerOnLongPush = Handler()
    private var onLongPushed: OnLongPushRunnable? = null

    enum class JoyStick {
        NONE, UP, UPRIGHT, RIGHT, DOWNRIGHT, DOWN, DOWNLEFT, LEFT, UPLEFT, LONGPUSH, MORE_UP, MORE_UPRIGHT, MORE_RIGHT, MORE_DOWNRIGHT, MORE_DOWN, MORE_DOWNLEFT, MORE_LEFT, MORE_UPLEFT;

        companion object {
            fun isMore(next: JoyStick, previous: JoyStick): Boolean {
                var isMore = false
                if (previous == UP && next == MORE_UP) isMore = true
                if (previous == RIGHT && next == MORE_RIGHT) isMore = true
                if (previous == DOWN && next == MORE_DOWN) isMore = true
                if (previous == LEFT && next == MORE_LEFT) isMore = true
                return isMore
            }

            fun isLess(next: JoyStick, previous: JoyStick): Boolean {
                var isMore = false
                if (next == UP && previous == MORE_UP) isMore = true
                if (next == RIGHT && previous == MORE_RIGHT) isMore = true
                if (next == DOWN && previous == MORE_DOWN) isMore = true
                if (next == LEFT && previous == MORE_LEFT) isMore = true
                return isMore
            }
        }
    }

    init {
        //        mContext = context;
        if (!isInEditMode) setZOrderOnTop(true)
        initHolder()
        res = context.resources
        loadImages(res)
        initAlphaPaints()
        jsEntity = JoyStickEntity()
        registerOnTouchEvent()
    }

    private fun initHolder() {
        surfaceHolder = holder
        surfaceHolder!!.addCallback(this)
        surfaceHolder!!.setFormat(PixelFormat.TRANSPARENT)
    }

    private fun initAlphaPaints() {
        alphaSigPaint = Paint()
        alphaBacksPaint = Paint()
        alphaStickPaint = Paint()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        init()
        val canvas = surfaceHolder!!.lockCanvas()
        drawBaseCanvas(canvas)
        drawStick(canvas)
        surfaceHolder!!.unlockCanvasAndPost(canvas)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}


    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    /**
     * DEFALUT
     * pad (params) 504 // 126 * 4
     * stick size   220 // 55 * 4
     * shadow size  252 // 63 * 4
     * offset       180 // 45 * 4
     * min distance 40  // 10 * 4
     * pad alpha    150
     * stick alpha  180
     */
    private fun init() {
        registerScreenSize()
        registerLayoutCenter(params!!.width, params!!.height)
        registerStickSize()
        stickTall = stickHeight / DENO_RATE_STICK_TALL_TO_SIZE // make user feel sticky
        setStickSize(params!!.width / DENO_RATE_STICK_SIZE_TO_PAD,
            params!!.height / DENO_RATE_STICK_SIZE_TO_PAD
        )
        layoutAlpha = ALPHA_PAD_DEFAULT
        stickAlpha = ALPHA_STICK_DEFAULT
        setSignalAlpha(ALPHA_SIGNAL_DEFAULT)
        offset = params!!.width / DENO_RATE_OFFSET_TO_PAD
        //minimumDistance = params!!.width / DENO_RATE_MIN_DISTANCE_TO_PAD
        resizeImages()
    }

    /**
     * this method should call after (or while) view creation
     *
     * getWidth(), getHeight() method return 0
     * in constructor JoyStickSurfaceView()
     */
    private fun registerScreenSize() {
        params = ViewGroup.LayoutParams(width, height)
    }

    /**
     * this size register method should call
     * after bitmap images is loaded.
     */
    private fun registerStickSize() {
        if (stick == null) return
        stickWidth = stick!!.width
        stickHeight = stick!!.height
    }

    private fun registerOnTouchEvent() {
        setOnTouchListener(object : OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                drawJoyStickWith(event)
                performPostLongPushEvent(event)
                if (stickState != JoyStick.NONE) {
                    handlerOnLongPush.removeCallbacks(onLongPushed!!)
                }
                if (event.action == MotionEvent.ACTION_DOWN
                    || event.action == MotionEvent.ACTION_MOVE
                ) {
                    if (distance > minimumDistance && jsEntity.isTouched()) {
                        performOnChangeState(judgeStateWith(angle, distance))
                        //                        performOnChangeState(judgeStateWith(angle));
                    } else if (distance <= minimumDistance && jsEntity.isTouched()) {
                        // STICK_NONE;
                        performReleaseJoyStick()
                    }
                } else {
                    handlerOnLongPush.removeCallbacks(onLongPushed!!)
                    performReleaseJoyStick()
                }
                return true
            }
        })
    }

    private fun performReleaseJoyStick() {
        setStickState(JoyStick.NONE)
        interruptJoyStickMoveThread()

        performOnJoyStickMove()
    }

    private fun performPostLongPushEvent(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN) {
            handlerOnLongPush.postDelayed(onLongPushed!!, TIME_LONG_PUSH_EVENT_ACTIVATE.toLong())
        }
    }

    private fun judgeStateWith(angle: Float, distance: Float): JoyStick {
        val midDistance = (params!!.width / 2 - offset).toFloat()
        val state = JoyStick.NONE
        if (angle >= 247.5 && angle < 292.5) {
            return if (distance > midDistance) JoyStick.MORE_UP else JoyStick.UP
        } else if (angle >= 292.5 && angle < 337.5) {
            return if (distance > midDistance) JoyStick.MORE_UPRIGHT else JoyStick.UPRIGHT
        } else if (angle >= 337.5 || angle < 22.5) {
            return if (distance > midDistance) JoyStick.MORE_RIGHT else JoyStick.RIGHT
        } else if (angle >= 22.5 && angle < 67.5) {
            return if (distance > midDistance) JoyStick.MORE_DOWNRIGHT else JoyStick.DOWNRIGHT
        } else if (angle >= 67.5 && angle < 112.5) {
            return if (distance > midDistance) JoyStick.MORE_DOWN else JoyStick.DOWN
        } else if (angle >= 112.5 && angle < 157.5) {
            return if (distance > midDistance) JoyStick.MORE_DOWNLEFT else JoyStick.DOWNLEFT
        } else if (angle >= 157.5 && angle < 202.5) {
            return if (distance > midDistance) JoyStick.MORE_LEFT else JoyStick.LEFT
        } else if (angle >= 202.5 && angle < 247.5) {
            return if (distance > midDistance) JoyStick.MORE_UPLEFT else JoyStick.UPLEFT
        }
        return state
    }

    private fun performOnChangeState(next: JoyStick) {
        if (stickState != next) {
            // change from other state
            performOnChangeStateFromOthers()
        }
        setStickState(next)
    }

    private fun performOnChangeStateFromOthers() {
        if (threadJoyStickMove != null && threadJoyStickMove!!.isAlive) {
            threadJoyStickMove!!.interrupt()
        }
        threadJoyStickMove = Thread {
            while (!Thread.interrupted()) {
                // why post ?
                post { performOnJoyStickMove() }
                try {
                    sleepJoyStick()
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
        threadJoyStickMove!!.start()
        performOnJoyStickMove()
    }

    @Throws(InterruptedException::class)
    private fun sleepJoyStick() {
        var interval = loopInterval
        if (hasFastLoop) interval = calCurrentInterval()
        Thread.sleep(interval)
    }

    private fun performOnJoyStickMove() {
        if (onJoyStickMoveListener != null) onJoyStickMoveListener!!.onValueChanged(
            jsEntity.x, jsEntity.y,
            getStickState()
        )
    }

    private fun interruptJoyStickMoveThread() {
        if (threadJoyStickMove != null) threadJoyStickMove!!.interrupt()
    }

    private fun registerLayoutCenter(width: Int, height: Int) {
        jsEntity.centerX = (width / 2).toFloat()
        jsEntity.centerY = (height / 2).toFloat()
    }

    private fun loadImages(res: Resources) {
        releaseJoyStickImages()
        loadImages(
            res,
            RESID_BACKGROUND_DEFAULT,
            RESID_STICK_DEFAULT,
            RESID_SHADOW_DEFAULT
        )
        // if you remove shadow, you should also remove "stickTall" : stickTall = 0
        if (canUseSignal) loadSignalImages(res)
    }

    private fun loadImages(
        res: Resources,
        resIdBacks: Int, resIdStick: Int, resIdShadow: Int
    ) {
        background = BitmapFactory.decodeResource(res, resIdBacks)
        stick = BitmapFactory.decodeResource(res, resIdStick)
    }

    private fun loadSignalImages(res: Resources) {
        releaseSignalImages()
        loadSignalImages(
            res,
            RESID_SIG_UP_DEFAULT,
            RESID_SIG_RIGHT_DEFAULT,
            RESID_SIG_DOWN_DEFAULT,
            RESID_SIG_LEFT_DEFAULT
        )
    }

    private fun loadSignalImages(
        res: Resources,
        resIdUp: Int, resIdRight: Int, resIdDown: Int, resIdLeft: Int
    ) {
        signalUp = BitmapFactory.decodeResource(res, resIdUp)
        signalRight = BitmapFactory.decodeResource(res, resIdRight)
        signalDown = BitmapFactory.decodeResource(res, resIdDown)
        signalLeft = BitmapFactory.decodeResource(res, resIdLeft)
    }

    private fun releaseJoyStickImages() {
        if (background != null) background!!.recycle()
        if (stick != null) stick!!.recycle()
    }

    private fun releaseSignalImages() {
        if (signalUp != null) signalUp!!.recycle()
        if (signalRight != null) signalRight!!.recycle()
        if (signalDown != null) signalDown!!.recycle()
        if (signalLeft != null) signalLeft!!.recycle()
    }

    private fun drawJoyStickWith(event: MotionEvent) {
        val canvas = surfaceHolder!!.lockCanvas()
        drawBaseCanvas(canvas)
        drawStick(canvas, event)
        surfaceHolder!!.unlockCanvasAndPost(canvas)
    }

    private fun drawBaseCanvas(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        drawBackground(canvas)
    }

    private fun drawStick(canvas: Canvas, event: MotionEvent) {
        positionX = (event.x - params!!.width / 2).toInt()
        positionY = (event.y - params!!.height / 2).toInt()
        distance =
            Math.sqrt(Math.pow(positionX.toDouble(), 2.0) + Math.pow(positionY.toDouble(), 2.0))
                .toFloat()
        angle = calAngle(positionX.toFloat(), positionY.toFloat()).toFloat()
        val midDistanceX = (params!!.width / 2 - offset).toFloat()
        val midDistanceY = (params!!.height / 2 - offset).toFloat()
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (distance <= midDistanceX) {
                jsEntity.position(event.x, event.y)
                jsEntity.setTouched(true)
            }
        } else if (event.action == MotionEvent.ACTION_MOVE && jsEntity.isTouched()) {
            if (distance <= midDistanceX) {
                jsEntity.position(event.x, event.y)
            } else {
                var x = (cos(
                    Math.toRadians(
                        calAngle(
                            positionX.toFloat(),
                            positionY.toFloat()
                        )
                    )
                ) * midDistanceX).toFloat()
                var y = (sin(
                    Math.toRadians(
                        calAngle(
                            positionX.toFloat(),
                            positionY.toFloat()
                        )
                    )
                ) * midDistanceY).toFloat()
                x += (params!!.width / 2).toFloat()
                y += (params!!.height / 2).toFloat()
                jsEntity.position(x, y)
            }
        } else if (event.action == MotionEvent.ACTION_UP) {
            // reset stick pad
            drawBaseCanvas(canvas)
            jsEntity.setTouched(false)
        }

        drawStick(canvas)
    }

    private fun drawStick(canvas: Canvas) {
        if (jsEntity.isTouched()) {
            canvas.drawBitmap(stick!!, jsEntity.x, jsEntity.y, alphaStickPaint)
        } else {
            canvas.drawBitmap(
                stick!!,
                jsEntity.centerX - stickWidth / 2,
                jsEntity.centerY - stickHeight / 2, alphaStickPaint
            )
        }
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawBitmap(background!!, 0f, 0f, alphaBacksPaint)
    }

   /* val posX: Int
        get() = jsEntity.y
    val posY: Int
        get() = jsEntity.y*/

    private fun getDistance(): Float {
        return if (distance < minimumDistance) distance else 100f
    }

    var layoutAlpha: Int
        get() = alphaLayout
        set(alpha) {
            alphaLayout = alpha
            alphaBacksPaint!!.alpha = alpha
        }
    var stickAlpha: Int
        get() = alphaStick
        set(alpha) {
            alphaStick = alpha
            alphaStickPaint!!.alpha = alpha
        }

    fun getStickState(): JoyStick {
        return stickState
    }

    private fun resizeImages() {
        stick = resizeImage(stick, stickWidth, stickHeight)
        background = resizeImage(background, params!!.width, params!!.height)
        if (canUseSignal) {
            resizeSignalImages()
        }
    }

    private fun resizeSignalImages() {
        signalUp = resizeImage(signalUp, params!!.width, params!!.height)
        signalRight = resizeImage(signalRight, params!!.width, params!!.height)
        signalDown = resizeImage(signalDown, params!!.width, params!!.height)
        signalLeft = resizeImage(signalLeft, params!!.width, params!!.height)
    }

    private fun resizeImage(original: Bitmap?, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(original!!, targetWidth, targetHeight, false)
    }

    private fun setStickState(next: JoyStick) {
        if (next != stickState) onChangeStateListener!!.onChangeState(next, stickState)
        stickState = next
    }

    fun setStickSize(width: Int, height: Int) {
        stickWidth = width
        stickHeight = height
    }

    fun setSignalAlpha(alpha: Int) {
        alphaSignal = alpha
        alphaSigPaint!!.alpha = alpha
    }

    private fun calAngle(x: Float, y: Float): Double {
        if (x >= 0 && y >= 0) return Math.toDegrees(Math.atan((y / x).toDouble())) else if (x < 0 && y >= 0) return Math.toDegrees(
            Math.atan((y / x).toDouble())
        ) + 180 else if (x < 0 && y < 0) return Math.toDegrees(Math.atan((y / x).toDouble())) + 180 else if (x >= 0 && y < 0) return Math.toDegrees(
            Math.atan((y / x).toDouble())
        ) + 360
        return 0.0
    }

    fun setOnJoyStickMoveListener(
        listener: OnJoystickMoveListener?,
        loopInterval: Long
    ) {
        onJoyStickMoveListener = listener
        this.loopInterval = loopInterval
        hasFastLoop = false
    }

    fun setOnJoyStickMoveListener(
        listener: OnJoystickMoveListener?,
        loopSlowInterval: Long, loopFastInterval: Long
    ) {
        setOnJoyStickMoveListener(listener, loopSlowInterval)
        this.loopFastInterval = loopFastInterval
        hasFastLoop = true
    }

    fun setOnLongPushListener(onLongPushListener: OnLongPushListener?) {
        onLongPushed = onLongPushListener?.let { OnLongPushRunnable(it) }
    }

    fun setOnChangeStateListener(onChangeStateListener: OnChangeStateListener?) {
        this.onChangeStateListener = onChangeStateListener
    }

    private fun calCurrentInterval(): Long {
        var `in` = loopInterval
        val midDistance = (params!!.width / 2 - offset).toFloat()
        if (distance <= midDistance) `in` = loopInterval else if (distance > midDistance) `in` =
            loopFastInterval
        return `in`
    }

    /**
     * Event Listeners (and Runnable)
     */
    interface OnLongPushListener {
        fun onLongPush()
    }

    interface OnChangeStateListener {
        fun onChangeState(next: JoyStick?, previous: JoyStick?)
    }

    interface OnJoystickMoveListener {
        fun onValueChanged(angle: Float, power: Float, state: JoyStick?)
    }

    private inner class OnLongPushRunnable(var listener: OnLongPushListener) :
        Runnable {
        override fun run() {
            performLongPushed()
        }

        private fun performLongPushed() {
            listener.onLongPush()
            setStickState(JoyStick.LONGPUSH)
        }
    }

    // seems to cause ERROR
    //    public interface OnJoyStickMoveListener {
    //        void onValueChanged(float angle, float power, JoyStickState direction);
    //    }
    private inner class JoyStickEntity {
        private var isTouched = false
        var x = 0f
        var y = 0f
        var centerX = 0f
        var centerY = 0f // center

        fun position(posx: Float, posy: Float) {
            x = posx - stickWidth / 2
            y = posy - stickHeight / 2
        }

        fun isTouched(): Boolean {
            return isTouched
        }

        fun setTouched(touched: Boolean) {
            isTouched = touched
        }

    }

    companion object {
        const val LOOP_INTERVAL_SLOW: Long = 800
        const val LOOP_INTERVAL_FAST: Long = 100
    }
}