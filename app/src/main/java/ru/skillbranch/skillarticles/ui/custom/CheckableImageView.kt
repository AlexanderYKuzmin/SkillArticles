package ru.skillbranch.skillarticles.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Checkable
import android.widget.ImageView

class CheckableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr),
    Checkable,
    View.OnClickListener {
    private var checked = false

    companion object {
        private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
    }

    init {
        setOnClickListener(this)
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) View.mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        return drawableState
    }

    override fun isChecked(): Boolean = checked

    override fun toggle() {
        isChecked = !checked
    }

    override fun setChecked(checked: Boolean) {
        if (this.checked == checked) return
        this.checked = checked
        refreshDrawableState()
    }

    override fun onClick(v: View?) {
        toggle()
    }
}