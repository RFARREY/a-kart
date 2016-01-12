package com.frogdesign.akart.view;

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import org.apache.commons.lang3.tuple.MutablePair

class TargetsView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : View(context, attrs, defStyleAttr, defStyleRes) {
    private val points: MutableMap<String, PointF> = hashMapOf()
    private val paint: Paint

    init {
        paint = Paint()
        paint.color = Color.RED
        paint.alpha = 128;
    }

    constructor(ctx: Context, attrs: AttributeSet, defStyleAttr: Int) : this(ctx, attrs, 0, 0) {
    }

    constructor(ctx: Context, attrs: AttributeSet) : this(ctx, attrs, 0, 0) {
    }

    constructor(ctx: Context) : this(ctx, null, 0, 0) {
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w: Int = MeasureSpec.getSize(widthMeasureSpec)
        val h: Int = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        for (b in points.values)
            if (b.x > Float.MIN_VALUE) canvas?.drawCircle(b.x, b.y, 10f, paint);
    }

    fun nullify() {
        for (b in points.values) {
            b.x = Float.MIN_VALUE
            b.x = Float.MIN_VALUE
        }
        invalidate()
    }

    fun setTarget(id: String, x: Float, y: Float) {
        var pair = points.get(id) ?: PointF()
        pair.x = x
        pair.y = y
        points.put(id, pair)
        invalidate()
    }
}