package ru.skillbranch.skillarticles.extensions

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop

fun View.setMarginOptionally(
    left: Int = marginLeft,
    right: Int = marginRight,
    top: Int = marginTop,
    bottom: Int = marginBottom
) {
    val lp = layoutParams as CoordinatorLayout.LayoutParams
    lp.setMargins(left, top, right, bottom)
    layoutParams = lp
}