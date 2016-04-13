package com.frogdesign.akart.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import com.frogdesign.akart.Comm
import com.frogdesign.akart.R
import com.jakewharton.rxbinding.widget.RxSeekBar
import com.jakewharton.rxbinding.widget.SeekBarChangeEvent
import com.jakewharton.rxbinding.widget.SeekBarStopChangeEvent
import rx.subjects.BehaviorSubject
import timber.log.Timber

class GasPedal(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : VerticalSeekBar(context, attrs, defStyleAttr, defStyleRes) {
    companion object {
        private val REST_POSITION = 30
        private val MAX_POSITION = 200
        private val resCompat = ResourcesCompat()
    }

    constructor(ctx: Context, attrs: AttributeSet, @Suppress("UNUSED_PARAMETER") defStyleAttr: Int) : this(ctx, attrs, 0, 0) {
        init()
    }

    constructor(ctx: Context, attrs: AttributeSet) : this(ctx, attrs, 0, 0) {
        init()
    }

    constructor(ctx: Context) : this(ctx, null, 0, 0) {
        init()
    }

    private val greenPaint = Paint()
    private var margin: Float = 0f

    val events = BehaviorSubject.create<SeekBarChangeEvent>()

    private fun init() {
        val margin = resources.getDimension(R.dimen.col_1)
        val marginInt = margin.toInt()
        setPadding(0, marginInt, 0, marginInt)
        this.margin = margin
        max = MAX_POSITION
        progress = REST_POSITION
        greenPaint.color = resCompat.getColor(resources, R.color.militaryGreen, null)
        greenPaint.strokeWidth = resources.getDimension(R.dimen.btn_stroke)

        RxSeekBar.changeEvents(this).subscribe { event ->
            Timber.d("change %s", event)
            //when user stop interact bring it back to 0
            if (event is SeekBarStopChangeEvent) progress = REST_POSITION

            //forward the event to event emitter
            events.onNext(event)
        }
    }

    val level : Float
        get() {
            val progress = this.progress - REST_POSITION
            if (progress == 0) return 0f
            else if (progress > 0) return progress.toFloat() / (MAX_POSITION - REST_POSITION)
            else /*if (progress < 0)*/ return progress.toFloat() / (REST_POSITION)
        }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val thumbH = thumb.intrinsicHeight
        val y = (height - thumbH) * (MAX_POSITION - REST_POSITION).toFloat() / MAX_POSITION + thumbH / 2

        val margin = this.margin
        c.drawLine(0f, y, margin, y, greenPaint)
        val widthFloat = width.toFloat()
        c.drawLine(widthFloat - margin, y, widthFloat, y, greenPaint)
    }
}