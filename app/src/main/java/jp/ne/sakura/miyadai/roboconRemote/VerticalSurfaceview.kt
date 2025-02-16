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
import android.view.ViewGroup
class VerticalSurfaceview(context: Context, attrs: AttributeSet?) :
    SurfaceView(context, attrs), SurfaceHolder.Callback{

    private lateinit var params : ViewGroup.LayoutParams
    private lateinit var background: Bitmap
    private lateinit var stick: Bitmap

    private var postionY : Float = 0f

    private var alphaBacksPaint : Paint
    private var alphaStickPaint : Paint

    private var isTouched = false
    private var islocked = false

    private lateinit var surfaceHolder : SurfaceHolder
    private val ALPHA_PAD_DEFAULT = 150
    private var alphaLayout = 200


    var layoutAlpha: Int
        get() = alphaLayout
        set(alpha) {
            alphaLayout = alpha
            alphaBacksPaint.alpha = alpha
        }

    val sendY : Float
        get() = if (isTouched) (postionY - params.height  / 2) / (params.height - params.width) * 2 else 0f

    fun sety(x : Float)
    {
        islocked = true
        postionY = (x * (params.height - params.width)/2) + height/2
        val canvas = surfaceHolder.lockCanvas()
        drawBackground(canvas)
        drawStick(canvas)
        surfaceHolder.unlockCanvasAndPost(canvas)
    }

    init {
        val res = context.resources
        alphaBacksPaint = Paint()
        alphaStickPaint = Paint()
        loadImages(res)
        initHolder()
        registerOnTouchEvent()
    }

    private fun loadImages(
        res: Resources,
    ) {
        background = BitmapFactory.decodeResource(res, R.drawable.vojoystick)
        stick = BitmapFactory.decodeResource(res, R.drawable.vjoystick_stick)
    }

    private fun initHolder() {
        surfaceHolder = holder
        surfaceHolder.addCallback(this)
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun registerOnTouchEvent() {
        setOnTouchListener { _, event ->
            drawJoyStickWith(event)
            true
        }
    }

    override fun surfaceCreated(surfaceholder: SurfaceHolder) {
        layoutAlpha = ALPHA_PAD_DEFAULT
        params = ViewGroup.LayoutParams(width, height)
        postionY = (params.height  / 2).toFloat()

        background =  Bitmap.createScaledBitmap(background, params.width, params.height, false)
        stick =  Bitmap.createScaledBitmap(stick, params.width, params.width, false)

        val canvas = surfaceHolder.lockCanvas()
        drawBackground(canvas)
        drawStick(canvas)
        surfaceHolder.unlockCanvasAndPost(canvas)
        setZOrderOnTop(true);
    }

    private fun drawStick(canvas: Canvas) {
        canvas.drawBitmap(stick,  0f, postionY - params.width /2 , alphaStickPaint)
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}

    override fun surfaceDestroyed(p0: SurfaceHolder) {}

    private fun drawJoyStickWith(event : MotionEvent) {
        val canvas = surfaceHolder.lockCanvas()
        drawBackground(canvas)
        drawStick(canvas, event)
        surfaceHolder.unlockCanvasAndPost(canvas)
    }

    private fun drawBackground(canvas: Canvas){
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        canvas.drawBitmap(background, 0f, 0f, alphaBacksPaint)
    }

    private fun drawStick(canvas: Canvas, event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_UP) {
            isTouched = false
            postionY  = (params.height  / 2).toFloat()
        } else {
            if (event.y > params.height - params.width / 2) {
                postionY = params.height - params.width / 2.0f
            } else if (event.y - params.width / 2 < 0) {
                postionY = params.width / 2.0f
            } else {
                postionY = event.y
            }

            if (event.action == MotionEvent.ACTION_DOWN) {
                isTouched = true
            }
        }
        drawStick(canvas)
    }
}