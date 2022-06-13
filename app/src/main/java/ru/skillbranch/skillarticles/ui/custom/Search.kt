package ru.skillbranch.skillarticles.ui.custom

import android.content.Context
import android.transition.Visibility
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.shape.MaterialShapeDrawable
import ru.skillbranch.skillarticles.R

class Search @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var isOpen = false
    init {
        View.inflate(context, R.layout.layout_search, this)
        val materialBg = MaterialShapeDrawable.createWithElevationOverlay(context)
        materialBg.elevation = elevation
        background = materialBg
    }

    fun open() {
        if (isOpen) return
        isOpen = true
        visibility = View.VISIBLE
    }

    fun close() {
        if(!isOpen || !isAttachedToWindow) return
        isOpen = false
        visibility = View.GONE
    }
}