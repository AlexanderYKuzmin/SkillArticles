package ru.skillbranch.skillarticles.ui.custom.markdown

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import kotlin.properties.Delegates

class MarkdownContentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    private lateinit var copyListener: (String) -> Unit
    private var elements: List<MarkdownElement> = emptyList()

    //for restore
    private var ids = arrayListOf<Int>()

    var textSize by Delegates.observable(14f) { _, old, value ->
        if (value == old) return@observable
        this.children.forEach {
            it as IMarkdownView
            it.fontSize = value
        }
    }

    var isLoading: Boolean = true
    private val padding = context.dpToIntPx(8)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var usedHeight = paddingTop
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)

        children.forEach {
            measureChild(it, widthMeasureSpec, heightMeasureSpec)
            usedHeight += it.measuredHeight
        }

        usedHeight += paddingBottom
        setMeasuredDimension(width, usedHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var usedHeight = paddingTop
        val bodyWidth = right - left - paddingLeft - paddingRight
        val left =  paddingLeft
        val right = paddingRight

        children.forEach {
            if (it is MarkdownTextView) {
                it.layout(
                    left - paddingLeft/2,
                    usedHeight,
                    r - paddingRight/2,
                    usedHeight + it.measuredHeight
                )
            } else {
                it.layout(
                    left,
                    usedHeight,
                    r,
                    usedHeight + it.measuredHeight
                )
            }
            usedHeight += it.measuredHeight
        }
    }

    fun setContent(content: List<MarkdownElement>) {
        if (elements.isNotEmpty()) return
        elements = content
        content.forEach {
            when (it) {
                is MarkdownElement.Text -> {
                    val tv = MarkdownTextView(context, textSize).apply {
                        setPadding(context.dpToIntPx(8), 0, context.dpToIntPx(8), 0)
                        setLineSpacing(fontSize * 0.5f, 1f)
                    }

                    MarkdownBuilder(context)
                        .markdownToSpan(it)
                        .run {
                            tv.setText(this, TextView.BufferType.SPANNABLE)
                        }
                    addView(tv)
                }

                is MarkdownElement.Image -> {
                    val iv = MarkdownImageView(
                        context,
                        textSize,
                        it.image.url,
                        it.image.text.toString(),
                        it.image.alt
                    )
                }

                is MarkdownElement.Scroll -> {
                    val sv = MarkdownCodeView(
                        context,
                        textSize,
                        it.blockCode.text.toString()
                    )
                    sv.copyListener = copyListener
                    addView(sv)
                }
            }
        }
    }

    fun renderSearchResult(searchResult: List<Pair<Int, Int>>) {
        var counter = 0
        children.forEach { view ->
            Log.d("MarkdownContentView", "Children counter: ${counter++}")
            view as IMarkdownView
            view.clearSearchResult()
        }

        if (searchResult.isEmpty()) return

        val bounds = elements.map { it.bounds }
        val result = searchResult.groupByBounds(bounds)

        children.forEachIndexed { index, view ->
            view as IMarkdownView
            //search for child with markdown element offset
            //Log.d("MarkdownContentView", "elements[$index] = ${elements[index]}, offset = ${elements[index].offset}")
            Log.d("MarkdownContentView", "elements size = ${elements.size}, bounds size = ${bounds.size}")
            Log.d("MarkdownContentView", "current view: ${view.accessibilityClassName}")
            Log.d("MarkdownContentView", "current index: $index")
            view.renderSearchResult(result[index], elements[index].offset)
        }
    }

    fun renderSearchPosition(searchPosition: Pair<Int, Int>?) {
        searchPosition ?: return
        val bounds = elements.map { it.bounds }

        val index = bounds.indexOfFirst { (start, end) ->
            val boundRange = start..end
            val (startPos, endPos) = searchPosition
            startPos in boundRange && endPos in boundRange
        }

        if (index == -1) return
        val view = getChildAt(index)
        view as IMarkdownView
        view.renderSearchPosition(searchPosition, elements[index].offset)
    }

    fun clearSearchResult() {
        children.forEach { view ->
            view as IMarkdownView
            view.clearSearchResult()
        }
    }

    fun setCopyListener(listener: (String) -> Unit) {
        copyListener = listener
    }
}




private fun List<Pair<Int, Int>>.groupByBounds(bounds: List<Pair<Int, Int>>): List<MutableList<Pair<Int, Int>>> {
    var resultList = mutableListOf<MutableList<Pair<Int, Int>>>()

    bounds.forEach { singleBounds ->
        var group = mutableListOf<Pair<Int, Int>>()
        val boundRange = singleBounds.first..singleBounds.second
        Log.d("MarkdowncontentView", "********************************")
        Log.d("MarkdowncontentView", "current bound range: ${boundRange}")
        forEach { item ->
            if (item.first in boundRange && item.second in boundRange) {
                group.add(item)
            }
        }
        Log.d("MarkdownContentView", "items in group = ${group.size}")
        resultList.add(group)
    }
    Log.d("MarkdownContentView", "group quantity = ${resultList.size}")
    return resultList
}
