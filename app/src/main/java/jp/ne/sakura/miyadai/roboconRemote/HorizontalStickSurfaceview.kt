package jp.ne.sakura.miyadai.roboconRemote

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup

class HorizontalStickSurfaceview(context: Context, attrs: AttributeSet?) :
    SurfaceView(context, attrs), SurfaceHolder.Callback{
    private lateinit var params : ViewGroup.LayoutParams

    private fun init() {
        params = ViewGroup.LayoutParams(width, height)
    }
    override fun surfaceCreated(surfaceholder: SurfaceHolder) {
        val canvas = surfaceholder.lockCanvas()
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        TODO("Not yet implemented")
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        TODO("Not yet implemented")
    }

    private fun drawBackground(canvas: Canvas){
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

}