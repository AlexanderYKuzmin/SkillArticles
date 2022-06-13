package ru.skillbranch.skillarticles.ui.custom.behaviors

import android.util.Log
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.math.MathUtils
import androidx.core.view.ViewCompat
import ru.skillbranch.skillarticles.ui.custom.Bottombar

class BottombarBehavior : CoordinatorLayout.Behavior<Bottombar>() {

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: Bottombar,
        directTargetChild: View,
        target: View,
        axes: Int
    ): Boolean {
        val move = axes == ViewCompat.SCROLL_AXIS_VERTICAL
        Log.d("BottombarBehavior", "move vertical? : $move")
        return move
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: Bottombar,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        child.translationY = MathUtils.clamp(child.translationY + dy, 0f, child.minHeight.toFloat())
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
    }
}