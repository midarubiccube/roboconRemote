package jp.ne.sakura.miyadai.roboconRemote

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class JoyStickSurfaceView(context: Context, attrs: AttributeSet?) :
    SurfaceView(context, attrs), SurfaceHolder.Callback {
    private val DENO_RATE_STICK_TALL_TO_SIZE = 25
    private val DENO_RATE_STICK_SIZE_TO_PAD = 2
    private val DENO_RATE_OFFSET_TO_PAD = 3
    private val ALPHA_PAD_DEFAULT = 150
    private val ALPHA_STICK_DEFAULT = 180
    private var alphaStick = 200
    private var alphaLayout = 200
    var offset = 0
    private var surfaceHolder: SurfaceHolder? = null
    private var params: ViewGroup.LayoutParams? = null
    private var stickTall = 0
    private var stickWidth = 0
    private var stickHeight = 0
    private var positionX = 0
    private var positionY = 0
    private var distance = 0f
    private val jsEntity // joy stick entity
            : JoyStickEntity
    private var alphaBacksPaint: Paint? = null
    private var alphaStickPaint: Paint? = null
    private val res: Resources
    private var background: Bitmap? = null
    private var stick: Bitmap? = null


    val getPosX : Float
        get() = if (jsEntity.isTouched()|| jsEntity.islock()) {
            (jsEntity.x - (jsEntity.centerX - stickWidth / 2)) / (params!!.width / 6)

        } else 0f

    val getPosY : Float
        get() = if (jsEntity.isTouched() || jsEntity.islock()) {
            (jsEntity.y - (jsEntity.centerY - stickHeight / 2)) / (params!!.height / 6)
        } else 0f

    fun setPOS(x : Float, y : Float){
        if (!jsEntity.isTouched()){
            jsEntity.x = (x * (params!!.width / 6) + (jsEntity.centerX - stickWidth / 2))
            jsEntity.y = (y * (params!!.height / 6) +(jsEntity.centerY - stickHeight / 2))
            val canvas = surfaceHolder!!.lockCanvas()
            jsEntity.setlock(true)
            drawBaseCanvas(canvas)
            canvas.drawBitmap(stick!!, jsEntity.x, jsEntity.y, alphaStickPaint)
            surfaceHolder!!.unlockCanvasAndPost(canvas)
        }
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

    init {
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

    private fun loadImages(res: Resources) {
        releaseJoyStickImages()
        background = BitmapFactory.decodeResource(res, R.drawable.s_joystick_base)
        stick = BitmapFactory.decodeResource(res, R.drawable.s_joystick_stick)
    }


    private fun initAlphaPaints() {
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
        offset = params!!.width / DENO_RATE_OFFSET_TO_PAD
        resizeImages()
    }

    private fun registerScreenSize() {
        params = ViewGroup.LayoutParams(width, height)
    }

    private fun registerStickSize() {
        if (stick == null) return
        stickWidth = stick!!.width
        stickHeight = stick!!.height
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun registerOnTouchEvent() {
        setOnTouchListener { _, event ->
            drawJoyStickWith(event)
            true
        }
    }

    private fun registerLayoutCenter(width: Int, height: Int) {
        jsEntity.centerX = (width / 2).toFloat()
        jsEntity.centerY = (height / 2).toFloat()
    }

    private fun releaseJoyStickImages() {
        if (background != null) background!!.recycle()
        if (stick != null) stick!!.recycle()
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
            sqrt(positionX.toDouble().pow(2.0) + positionY.toDouble().pow(2.0))
                .toFloat()
        val midDistanceX = (params!!.width / 2 - offset).toFloat()
        val midDistanceY = (params!!.height / 2 - offset).toFloat()
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (distance <= midDistanceX) {
                jsEntity.position(event.x, event.y)
                jsEntity.setlock(false)
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
            jsEntity.position(0f,0f)
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

    private fun resizeImages() {
        stick = resizeImage(stick, stickWidth, stickHeight)
        background = resizeImage(background, params!!.width, params!!.height)
    }

    private fun resizeImage(original: Bitmap?, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(original!!, targetWidth, targetHeight, false)
    }

    fun setStickSize(width: Int, height: Int) {
        stickWidth = width
        stickHeight = height
    }

    private fun calAngle(x: Float, y: Float): Double {
        if (x >= 0 && y >= 0) return Math.toDegrees(atan((y / x).toDouble())) else if (x < 0 && y >= 0) return Math.toDegrees(
            atan((y / x).toDouble())
        ) + 180 else if (x < 0 && y < 0) return Math.toDegrees(atan((y / x).toDouble())) + 180 else if (x >= 0 && y < 0) return Math.toDegrees(
            atan((y / x).toDouble())
        ) + 360
        return 0.0
    }

    private inner class JoyStickEntity {
        private var isTouched = false
        private var islock = false
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

        fun islock(): Boolean {
            return islock
        }

        fun setlock(lock: Boolean) {
            islock = lock
        }
    }
}