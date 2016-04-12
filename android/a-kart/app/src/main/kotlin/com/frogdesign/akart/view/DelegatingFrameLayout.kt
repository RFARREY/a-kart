package com.frogdesign.akart.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout


class DelegatingFrameLayout(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    constructor(ctx: Context, attrs: AttributeSet, @Suppress("UNUSED_PARAMETER") defStyleAttr: Int) : this(ctx, attrs, 0, 0) {
    }

    constructor(ctx: Context, attrs: AttributeSet) : this(ctx, attrs, 0, 0) {
    }

    constructor(ctx: Context) : this(ctx, null, 0, 0) {
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        val targetchild = getChildAt(0)
        if (ev != null && targetchild != null) {
            ev.offsetLocation(-targetchild.x, -targetchild.y)
            return targetchild.onTouchEvent(ev)
        } else return false
    }
}