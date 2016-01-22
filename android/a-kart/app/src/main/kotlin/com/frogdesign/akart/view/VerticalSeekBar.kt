package com.frogdesign.akart.view


import android.content.Context
import android.graphics.Canvas
import android.support.v7.widget.AppCompatSeekBar
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.SeekBar

/**
 * taken from https://github.com/jeisfeld/Augendiagnose/tree/master/AugendiagnoseIdea/augendiagnoseLib
 * Implementation of an easy vertical SeekBar, based on the normal SeekBar.
 */
class VerticalSeekBar : AppCompatSeekBar {

    /**
     * A change listener registrating start and stop of tracking. Need an own listener because the listener in SeekBar
     * is private.
     */
    private var mOnSeekBarChangeListener: SeekBar.OnSeekBarChangeListener? = null

    /**
     * Standard constructor to be implemented for all views.

     * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
     * *
     * @see android.view.View.View
     */
    constructor(context: Context) : super(context) {
    }

    /**
     * Standard constructor to be implemented for all views.

     * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
     * *
     * @param attrs   The attributes of the XML tag that is inflating the view.
     * *
     * @see android.view.View.View
     */
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    /**
     * Standard constructor to be implemented for all views.

     * @param context  The Context the view is running in, through which it can access the current theme, resources, etc.
     * *
     * @param attrs    The attributes of the XML tag that is inflating the view.
     * *
     * @param defStyle An attribute in the current theme that contains a reference to a style resource that supplies default
     * *                 values for the view. Can be 0 to not look for defaults.
     * *
     * @see android.view.View.View
     */
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
    }

    /*
     * (non-Javadoc) ${see_to_overridden}
     */
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(height, width, oldHeight, oldWidth)
    }

    /*
     * (non-Javadoc) ${see_to_overridden}
     */
    @Synchronized override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredHeight, measuredWidth)
    }

    /*
     * (non-Javadoc) ${see_to_overridden}
     */
    override fun onDraw(c: Canvas) {
        c.save()
        c.rotate(ROTATION_ANGLE.toFloat())
        c.translate((-height).toFloat(), 0f)

        super.onDraw(c)
        c.restore()
    }

    /*
     * (non-Javadoc) ${see_to_overridden}
     */
    override fun setOnSeekBarChangeListener(l: SeekBar.OnSeekBarChangeListener) {
        mOnSeekBarChangeListener = l
        super.setOnSeekBarChangeListener(l)
    }

    /*
     * (non-Javadoc) ${see_to_overridden}
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                progress = max - (max * event.y / height).toInt()
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener!!.onStartTrackingTouch(this)
                }
            }

            MotionEvent.ACTION_MOVE -> progress = max - (max * event.y / height).toInt()

            MotionEvent.ACTION_UP -> {
                progress = max - (max * event.y / height).toInt()
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener!!.onStopTrackingTouch(this)
                }
            }

            MotionEvent.ACTION_CANCEL -> if (mOnSeekBarChangeListener != null) {
                mOnSeekBarChangeListener!!.onStopTrackingTouch(this)
            }

            else -> {
            }
        }

        return true
    }

    /*
     * (non-Javadoc) ${see_to_overridden}
     */
    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        onSizeChanged(width, height, 0, 0)
    }

    companion object {
        /**
         * The angle by which the SeekBar view should be rotated.
         */
        private val ROTATION_ANGLE = -90
    }
}