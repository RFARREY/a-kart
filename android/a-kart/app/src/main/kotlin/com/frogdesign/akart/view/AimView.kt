package com.frogdesign.akart.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.widget.Scroller
import com.frogdesign.akart.R
import com.frogdesign.akart.util.ResourcesCompatInstance
import com.frogdesign.akart.util.clamp
import com.frogdesign.akart.util.dpToPx
import uk.co.chrisjenx.calligraphy.TypefaceUtils
import kotlin.properties.Delegates

class AimView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : View(context, attrs, defStyleAttr, defStyleRes) {
    companion object {
        val SIZE = 80.0f //dp
        val RADIUS = 100.0f //dp
        val STROKE = 3.0f //dp
    }

    private val points: MutableMap<String, PointF> = hashMapOf()
    private val boxPoints: MutableMap<String, PointF> = hashMapOf()
    private var size = AimView.SIZE.toInt()
    private var radius = AimView.RADIUS
    private var stroke = AimView.STROKE
    private var color = Color.RED
    private var textSize = 24f
    private val paint: Paint
    private val textPaint: Paint
    private var scroller: Scroller  by Delegates.notNull()
    val targetedIds = mutableListOf<String>()
    val boxIds = mutableListOf<String>()

    fun horizonRotate(c: Float) {
        targetRotation = c
        invalidate()
    }

    private var targetRotation = 0f
    private var actualRotation = 0f
    private var actualVelocity = 0f

    init {
        if (context != null) {
            size = dpToPx(context, AimView.SIZE)
            radius = dpToPx(context, AimView.RADIUS).toFloat()
            stroke = resources.getDimension(R.dimen.btn_stroke)
            color = ResourcesCompatInstance.getColor(resources, R.color.militaryGreen, null)
            textSize = dpToPx(context, 12f).toFloat()
            scroller = Scroller(context)
        }
        paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.color = color
        paint.isAntiAlias = true
        paint.textSize = textSize

        textPaint = Paint()
        textPaint.typeface = TypefaceUtils.load(context?.assets, "fonts/Menlo-Bold.ttf")
        textPaint.textSize = textSize
        textPaint.isAntiAlias = true
        textPaint.color = color
    }

    constructor(ctx: Context, attrs: AttributeSet, @Suppress("UNUSED_PARAMETER") defStyleAttr: Int) : this(ctx, attrs, 0, 0) {
    }

    constructor(ctx: Context, attrs: AttributeSet) : this(ctx, attrs, 0, 0) {
    }

    constructor(ctx: Context) : this(ctx, null, 0, 0) {
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(size, widthMeasureSpec)
        val h = resolveSize(size, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) return
        var cx = (width / 2).toFloat()
        var cy = (height / 2).toFloat()
        targetedIds.clear()
        boxIds.clear()
        var targeted = false
        for ((k, b) in points.entries) {
            if (b.x > Float.MIN_VALUE) {
                //Timber.i("Drawing %s, %s", k, b)
                canvas?.drawCircle(b.x, b.y, 10f, paint)
            };
            var distance = hypot(cx - b.x, cy - b.y)


            if (distance < radius) {
                targetedIds.add(k)
            }
        }
        for ((k, b) in boxPoints.entries) {
            if (b.x > Float.MIN_VALUE) {
                //Timber.i("Drawing %s, %s", k, b)
                canvas?.drawCircle(b.x, b.y, 10f, paint)
            };
            var distance = hypot(cx - b.x, cy - b.y)


            if (distance < radius) {
                boxIds.add(k)
            }
        }

        if (boxIds.isNotEmpty() || targetedIds.isNotEmpty()) {
            paint.alpha = 60
            paint.style = Paint.Style.FILL
            canvas?.drawCircle(cx, cy, radius, paint)
            paint.alpha = 255
            paint.style = Paint.Style.STROKE
            targeted = true
        }
        drawAim(canvas, cx, cy, targeted)
        var delta = targetRotation - actualRotation
        if (Math.abs(delta) > 0) {
            actualVelocity = delta / 6

            actualVelocity = clamp(actualVelocity, -10f, +10f)
            if (Math.abs(actualVelocity) > 1) {
                actualRotation += actualVelocity
                invalidate()
            }
        }
    }

    private fun drawAim(canvas: Canvas, cx: Float, cy: Float, targeted: Boolean) {
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(cx, cy, radius, paint)


        canvas.save(Canvas.MATRIX_SAVE_FLAG)
        canvas.rotate(actualRotation, cx, cy)
        if (targeted) {
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx - radius / 3 * 2, cy, stroke * 2, paint)
            canvas.drawCircle(cx + radius / 3 * 2, cy, stroke * 2, paint)
            paint.style = Paint.Style.STROKE
            canvas.drawText("TARGET ACQUIRED", cx + radius * 1.1f, cy, textPaint)
        }

        var segment = radius / 4
        canvas.drawLine(cx - segment, cy - segment, cx + segment, cy - segment, paint)
        canvas.drawLine(cx - segment, cy + segment, cx + segment, cy + segment, paint)
        segment = radius / (4 * 5) * 3
        canvas.drawLine(cx - segment, cy - segment, cx + segment, cy - segment, paint)
        canvas.drawLine(cx - segment, cy + segment, cx + segment, cy + segment, paint)
        segment = radius / (4 * 5)
        canvas.drawLine(cx - segment, cy - segment, cx + segment, cy - segment, paint)
        canvas.drawLine(cx - segment, cy + segment, cx + segment, cy + segment, paint)

        var path = Path()
        val littleRadius = stroke * 3
        val littleSegment = radius / 3
        val littleBaseY = radius / 4 * 3
        path.moveTo(cx - littleSegment, cy - littleBaseY)
        path.lineTo(cx - littleRadius, cy - littleBaseY)
        path.arcTo(cx - littleRadius, cy - littleBaseY - littleRadius, cx + littleRadius, cy - littleBaseY + littleRadius, 180f, 180f, false)
        path.lineTo(cx + littleSegment, cy - littleBaseY)

        canvas.drawPath(path, paint)
        canvas.save(Canvas.MATRIX_SAVE_FLAG)
        canvas.rotate(180f, cx, cy)
        canvas.drawPath(path, paint)
        canvas.restore()

        canvas.restore()

    }

    private fun hypot(x: Float, y: Float) = Math.hypot(x.toDouble(), y.toDouble()).toFloat()

    fun nullify() {
        for (b in points.values) {
            b.x = Float.MIN_VALUE
            b.y = Float.MIN_VALUE
        }
        for (b in boxPoints.values) {
            b.x = Float.MIN_VALUE
            b.y = Float.MIN_VALUE
        }
        invalidate()
    }

    fun setTarget(id: String, x: Float, y: Float) {
        var pair = points[id] ?: PointF()
        pair.x = x
        pair.y = y
        points.put(id, pair)
        invalidate()
    }

    fun setBox(id: String, x: Float, y: Float) {
        var pair = points[id] ?: PointF()
        pair.x = x
        pair.y = y
        boxPoints.put(id, pair)
        invalidate()
    }
}

