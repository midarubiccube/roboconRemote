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
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import java.lang.Math.abs

class HorizontalStickSurfaceview(context: Context, attrs: AttributeSet?) :
    SurfaceView(context, attrs), SurfaceHolder.Callback{

    private lateinit var params : ViewGroup.LayoutParams
    private lateinit var background: Bitmap
    private lateinit var stick: Bitmap

    private var X : Float = 0f

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

    val getX : Float
        get() = if (isTouched || islocked) (X - width  / 2) / (params.width - params.height) * 2 else 0f

    fun setx(x : Float)
    {
        islocked = true
        X = (x * (params.width - params.height)/2) + width/2
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
        background = BitmapFactory.decodeResource(res, R.drawable.hojoystick)
        stick = BitmapFactory.decodeResource(res, R.drawable.h_joystick_stick)
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
        X = (params.width  / 2).toFloat()

        background =  Bitmap.createScaledBitmap(background, params.width, params.height, false)
        stick =  Bitmap.createScaledBitmap(stick, params.height, params.height, false)

        val canvas = surfaceHolder.lockCanvas()
        drawBackground(canvas)
        drawStick(canvas)
        surfaceHolder.unlockCanvasAndPost(canvas)
        setZOrderOnTop(true);
    }

    private fun drawStick(canvas: Canvas) {
        canvas.drawBitmap(stick,  X - params.height /2 , 0f, alphaStickPaint)
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
        if (event.action == MotionEvent.ACTION_DOWN) {
            isTouched = true
            islocked = false
            if (event.x < params.width - params.height / 2 && event.x - params.height / 2 > 0){
                X  = event.x
            }
        } else if (event.action == MotionEvent.ACTION_MOVE && isTouched) {
            if (event.x < params.width - params.height / 2 &&  event.x - params.height / 2 > 0){
                X  = event.x
            }
        } else if (event.action == MotionEvent.ACTION_UP) {
            isTouched = false
            X  = (width  / 2).toFloat()
        }

        drawStick(canvas)
    }
}