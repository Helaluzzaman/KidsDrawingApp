package com.learning.kidsdrawingapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View


@Suppress("UNREACHABLE_CODE")
class DrawingView (context: Context, attrs: AttributeSet ): View(context, attrs) {
    private var mDrawPath :CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 20.toFloat()
    var color = Color.BLACK
    private val mPaths= ArrayList<CustomPath>()
    private val mUndoPaths = ArrayList<CustomPath>()

//    private var cirX: Float = 0f
//    private var cirY: Float = 0f
//    private var cirRad: Float = 100f
//    private var rect1 = RectF(20f,20f,500f,500f)
    fun undoOnClick(){
        if(mPaths.size >0){
            mUndoPaths.add(mPaths[mPaths.lastIndex])
            mPaths.removeAt(mPaths.lastIndex)
            invalidate()
        }
    }
    fun reduOnClick(){
        if(mUndoPaths.size >0){
            mPaths.add(mUndoPaths.removeAt(mUndoPaths.lastIndex))
            invalidate()
        }
    }
    fun undoAllonClick(): Boolean{
        if(mPaths.size >0){
            mPaths.clear()
            mUndoPaths.clear()
            invalidate()
        }
        return true
    }



    init {
        setupDrawingBoard()
    }

    private fun setupDrawingBoard() {
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint = Paint()
        mDrawPaint!!.isAntiAlias = true
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)   // resizable canvas making.
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawBitmap(mCanvasBitmap!!,0f,0f,mDrawPaint)

        for( path in mPaths){
            mDrawPaint!!.color = path.color
            mDrawPaint!!.strokeWidth = path.brushThickness
            canvas?.drawPath(path ,mDrawPaint!!)
            canvas?.drawPoint(path.x!!, path.y!!, mDrawPaint!!)

        }
        if(mDrawPath !=null){
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas?.drawPath(mDrawPath!!, mDrawPaint!!)
        }
//        if(cirX != 0f){
////            canvas?.drawCircle(cirX,cirY,cirRad,mDrawPaint!!)
////            canvas?.drawRect(cirX-100f,cirY-50f, 100f+cirX,100f+cirY,mDrawPaint!!)
//        }
//        if(touch2X != null && touch2Y != null){
//            canvas?.drawCircle(touch2X!!, touch2Y!!, mBrushSize/2, Paint())
//        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY= event?.y
        when(event?.action){
            MotionEvent.ACTION_DOWN-> {
//                 see where this thing will need.......todo
                mDrawPath?.color = color
                mDrawPath?.brushThickness = mBrushSize
                mDrawPath?.reset()
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath?.moveTo(touchX, touchY)
                        mDrawPath?.x = touchX
                        mDrawPath?.y = touchY
//                        mPoints.add(customPoint(touchX, touchY, color, mBrushSize))
                    }
                }
            }
            MotionEvent.ACTION_MOVE ->{
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath?.lineTo(touchX, touchY)
                    }
                }
            }
            MotionEvent.ACTION_UP ->{
                mPaths.add(mDrawPath!!)
                mDrawPath = CustomPath(color, mBrushSize)
            }
            else-> return false
        }
        invalidate()
        return true
    }
    fun setBrushSize( newSize: Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics)
        mDrawPath!!.brushThickness = mBrushSize
    }
    fun setColor(newColor: String){
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color
    }

    inner class CustomPath(var color: Int,
                           var brushThickness: Float) : Path() {
        var x: Float?= null
        var y: Float? = null
    }
}