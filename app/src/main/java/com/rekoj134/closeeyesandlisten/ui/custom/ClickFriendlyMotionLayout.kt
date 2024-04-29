package com.rekoj134.closeeyesandlisten.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.constraintlayout.motion.widget.MotionLayout

class ClickFriendlyMotionLayout: MotionLayout {
    var startTime = 0L

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
//        Log.e("ANCUTKO", event.action.toString())
//        if (event.action == MotionEvent.ACTION_DOWN) {
//            startTime = event.eventTime
//        }
//
//        if (event.action == MotionEvent.ACTION_MOVE && event.eventTime.minus(startTime) >= 80) {
//            return super.onInterceptTouchEvent(event)
//        }

        return false
    }
}