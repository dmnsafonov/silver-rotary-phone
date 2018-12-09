package net.dimanss47.swpersona

import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class PersonListOnItemTouchListener(
    private val activity: MainActivity,
    private val recyclerView: RecyclerView
) : RecyclerView.SimpleOnItemTouchListener() {
    private val gestureDetector = GestureDetector(
        activity.applicationContext,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean = true
        }
    )

    init { gestureDetector.setIsLongpressEnabled(false) }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        gestureDetector.onTouchEvent(e)
    }

    // TODO: ripple, "selected" color
    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        val childView = recyclerView.findChildViewUnder(e.x, e.y)
            ?: return false
        val viewHolder = recyclerView.getChildViewHolder(childView)
        if(!gestureDetector.onTouchEvent(e)) return false
        val url = (viewHolder as PersonListViewHolder).url
            ?: return false
        activity.openDetails(url)
        return true
    }
}
