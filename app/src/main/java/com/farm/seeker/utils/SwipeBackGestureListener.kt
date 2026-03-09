package com.farm.seeker.utils

import android.app.Activity
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

class SwipeBackGestureListener(private val activity: Activity) : GestureDetector.SimpleOnGestureListener() {
    private val SWIPE_THRESHOLD = 100
    private val SWIPE_VELOCITY_THRESHOLD = 100

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (e1 == null) return false
        val distanceX = e2.x - e1.x
        val distanceY = e2.y - e1.y
        if (abs(distanceX) > abs(distanceY) &&
            abs(distanceX) > SWIPE_THRESHOLD &&
            abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
            // Support both Left->Right and Right->Left swipes for "Back"
            activity.finish()
            return true
        }
        return false
    }
}
