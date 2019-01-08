package isf.seamenstaxi_r.Entities

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

class GestureListener(view: View) {

    init {

        var views = emptyArray<View>()
        views = views.plus(view)

        val childCount = (view as ViewGroup).childCount
        for (i in 0 until childCount) {
            views = views.plus(view.getChildAt(i))
        }

        var viewsAdd = emptyArray<View>()
        for (i in views) {
            if (i is ViewGroup) {
                val count = i.childCount
                for (j in 0 until count) {
                    viewsAdd = viewsAdd.plus(i.getChildAt(j))
                }
            }
        }

        views = views.plus(viewsAdd)

        views.forEach {

            val gd = GestureDetector(it.context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
                    val x1 = e1?.rawX
                    val y1 = e1?.rawY
                    val x2 = e2?.rawX
                    val y2 = e2?.rawY
                    if (x1 != null && x2 != null && y1 != null && y2 != null)
                        when (cAction) {
                            "left" -> if (x1 - x2 > 0) left((x1 - x2) * (-1))
                            "down" -> if (y1 - y2 < 0) down((y1 - y2) * (-1))
                            "x" -> left((x1 - x2) * (-1))
                        }

                    return super.onScroll(e1, e2, distanceX, distanceY)
                }

                override fun onSingleTapUp(e: MotionEvent?): Boolean {
                    onTap()
                    return super.onSingleTapUp(e)
                }
            })

            /*----ON UP----*/
            it.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP)
                    when (cAction) {
                        "left" -> view.translationX = 0f
                        "down" -> view.translationY = 0f
                        "x" -> view.translationX = 0f
                    }

                return@setOnTouchListener gd.onTouchEvent(event)
            }
        }

    }

    //private var cView = view
    //private var cLimit: Float = 0f
    private var cAction = ""
    private var onAction: ((dXY: Float) -> Unit)? = null
    var onTap: (() -> Unit) = {}
    var onEndGesture: (() -> Unit) = {}

    fun setOnX(action: (dXY: Float) -> Unit) {
        onAction = action
        cAction = "x"
        //cLimit = dX
    }

    fun setOnDown(action: (dXY: Float) -> Unit) {
        onAction = action
        cAction = "down"
        //cLimit = dY
    }

    /*fun clearGesture() {
        onAction = null
        cAction = ""
        //cLimit = 0f
    }*/

    private fun left(dX: Float) {
        //cView.translationX = dX
        //if (dX < cLimit)
        onAction?.let {
            it(dX)
        }
    }

    private fun down(dY: Float) {
        //cView.translationY = dY
        //if (dY > cLimit)
        onAction?.let {
            it(dY)
        }
    }

}
