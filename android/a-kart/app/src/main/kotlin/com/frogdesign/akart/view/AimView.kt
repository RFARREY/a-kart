package com.frogdesign.akart.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.frogdesign.akart.R
import com.frogdesign.akart.util.ResourcesCompatInstance
import com.frogdesign.akart.util.clamp
import com.frogdesign.akart.util.dpToPx
import com.frogdesign.akart.util.emptyAttributeSet
import uk.co.chrisjenx.calligraphy.TypefaceUtils

class AimView(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : View(context, attrs, defStyleAttr, defStyleRes) {
    companion object {
        val SIZE = 80.0f //dp
        val RADIUS = 100.0f //dp
        val STROKE = 3.0f //dp
        val TEXT_SIZE = 12.0f //dp
    }

    private val points: MutableMap<String, PointF> = hashMapOf()
    private val boxPoints: MutableMap<String, PointF> = hashMapOf()
    private var size = SIZE.toInt()
    private var stroke = STROKE
    private var radius = RADIUS
    private var textSize = TEXT_SIZE
    private var color = Color.RED
    private val paint: Paint
    private val textPaint: Paint
    private var cx: Float = 0f
    private var cy: Float = 0f
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
        size = dpToPx(context, SIZE)
        radius = dpToPx(context, RADIUS).toFloat()
        textSize = dpToPx(context, TEXT_SIZE).toFloat()
        stroke = resources.getDimension(R.dimen.btn_stroke)
        color = ResourcesCompatInstance.getColor(resources, R.color.militaryGreen, null)

        paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.color = color
        paint.isAntiAlias = true
        paint.textSize = textSize

        textPaint = Paint()
        textPaint.typeface = TypefaceUtils.load(context.assets, "fonts/Menlo-Bold.ttf")
        textPaint.textSize = textSize
        textPaint.isAntiAlias = true
        textPaint.color = color
    }

    constructor(ctx: Context, attrs: AttributeSet, @Suppress("UNUSED_PARAMETER") defStyleAttr: Int) : this(ctx, attrs, 0, 0)

    constructor(ctx: Context, attrs: AttributeSet) : this(ctx, attrs, 0, 0)

    constructor(ctx: Context) : this(ctx, emptyAttributeSet, 0, 0)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(size, widthMeasureSpec)
        val h = resolveSize(size, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cx = (width / 2).toFloat()
        cy = (height / 2).toFloat()
        initializeAimPath()
    }

    override fun onDraw(canvas: Canvas) {
        targetedIds.clear()
        boxIds.clear()
        var targeted = false
        for ((k, b) in points.entries) {
            if (b.x > Float.MIN_VALUE) {
                //Timber.i("Drawing %s, %s", k, b)
                canvas.drawCircle(b.x, b.y, 10f, paint)
            };
            var distance = hypot(cx - b.x, cy - b.y)


            if (distance < radius) {
                targetedIds.add(k)
            }
        }
        for ((k, b) in boxPoints.entries) {
            if (b.x > Float.MIN_VALUE) {
                //Timber.i("Drawing %s, %s", k, b)
                canvas.drawCircle(b.x, b.y, 10f, paint)
            };
            var distance = hypot(cx - b.x, cy - b.y)


            if (distance < radius) {
                boxIds.add(k)
            }
        }

        if (boxIds.isNotEmpty() || targetedIds.isNotEmpty()) {
            paint.alpha = 60
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, radius, paint)
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

    private val aimPath = Path()

    private fun initializeAimPath() {
        var segment = radius / 4
        var y = cy - segment
        aimPath.moveTo(cx - segment, y)
        aimPath.lineTo(cx + segment, y)

        segment = radius / (4 * 5) * 3
        y = cy - segment
        aimPath.moveTo(cx - segment, y)
        aimPath.lineTo(cx + segment, y)

        segment = radius / (4 * 5)
        y = cy - segment
        aimPath.moveTo(cx - segment, y)
        aimPath.lineTo(cx + segment, y)

        val littleRadius = stroke * 3
        val littleSegment = radius / 3
        val littleBaseY = radius / 4 * 3
        y = cy - littleBaseY
        aimPath.moveTo(cx - littleSegment, y)
        aimPath.lineTo(cx - littleRadius, y)
        aimPath.arcTo(cx - littleRadius, y - littleRadius, cx + littleRadius, cy - littleBaseY + littleRadius, 180f, 180f, false)
        aimPath.lineTo(cx + littleSegment, y)
    }

    private fun drawAim(canvas: Canvas, cx: Float, cy: Float, targeted: Boolean) {
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(cx, cy, radius, paint)
        canvas.rotate(actualRotation, cx, cy)
        if (targeted) {
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx - radius / 3 * 2, cy, stroke * 2, paint)
            canvas.drawCircle(cx + radius / 3 * 2, cy, stroke * 2, paint)
            paint.style = Paint.Style.STROKE
            canvas.drawText("TARGET ACQUIRED", cx + radius * 1.1f, cy, textPaint)
        }

        canvas.drawPath(aimPath, paint)
        canvas.save(Canvas.MATRIX_SAVE_FLAG)
        canvas.rotate(180f, cx, cy)
        canvas.drawPath(aimPath, paint)
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

