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
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup

class HorizontalStickSurfaceview(context: Context, attrs: AttributeSet?) :
    SurfaceView(context, attrs), SurfaceHolder.Callback{

    private lateinit var params : ViewGroup.LayoutParams
    private lateinit var background: Bitmap
    private lateinit var stick: Bitmap
    private var alphaBacksPaint : Paint
    private lateinit var surfaceHolder : SurfaceHolder

    init {
        val res = context.resources
        alphaBacksPaint = Paint()
        loadImages(res)
        initHolder()
    }

    private fun initHolder() {
        surfaceHolder = holder
        surfaceHolder.addCallback(this)
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT)
    }


    override fun surfaceCreated(surfaceholder: SurfaceHolder) {
        init()
        val canvas = surfaceHolder.lockCanvas()
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        canvas.drawBitmap(background, 0f, 0f, alphaBacksPaint)
        surfaceHolder.unlockCanvasAndPost(canvas)
    }

    private fun init() {
        params = ViewGroup.LayoutParams(width, height)
        background =  Bitmap.createScaledBitmap(background, params.width, params.height, false)
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
    }

    private fun loadImages(
        res: Resources,
    ) {
        background = BitmapFactory.decodeResource(res, R.drawable.hojoystick)
        //stick = BitmapFactory.decodeResource(res, resIdStick)
    }

    private fun drawBackground(canvas: Canvas){
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }



}