package ru.skillbranch.skillarticles.ui.custom.markdown

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.Layout
import android.text.Spanned
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.ColorUtils
import androidx.core.text.getSpans
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.*
import ru.skillbranch.skillarticles.ui.custom.spans.HeaderSpan
import ru.skillbranch.skillarticles.ui.custom.spans.SearchFocusSpan
import ru.skillbranch.skillarticles.ui.custom.spans.SearchSpan

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
class SearchBgHelper(
    context: Context,
    private val focusListener: (top: Int, bottom: Int) -> Unit,
    mockDrawable: Drawable?,
    mockDrawableLeft: Drawable?,
    mockDrawableMiddle: Drawable?,
    mockDrawableRight: Drawable?,
) {
    //primary constructor
    constructor(
        context: Context,
        focusListener: ((top: Int, bottom: Int) -> Unit)
    ) : this(
        context = context,
        focusListener = focusListener,
        mockDrawable = null,
        mockDrawableLeft = null,
        mockDrawableMiddle = null,
        mockDrawableRight = null,
    )

    private val padding: Int = context.dpToIntPx(4)
    private val radius: Float = context.dpToPx(8)
    private val borderWidth: Int = context.dpToIntPx(1)

    private val secondaryColor: Int = context.attrValue(R.attr.colorSecondary)
    private val alphaColor: Int = ColorUtils.setAlphaComponent(secondaryColor, 160)

    val drawable: Drawable = mockDrawable ?: GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadii = FloatArray(8).apply { fill(radius, 0, size) }
        color = ColorStateList.valueOf(alphaColor)
        setStroke(borderWidth, secondaryColor)
    }

    val drawableLeft: Drawable = mockDrawableLeft ?: GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadii = floatArrayOf(
            radius, radius, // top left
            0f, 0f, // top right
            0f, 0f, // bottom right
            radius, radius //bottom left
        )
        color = ColorStateList.valueOf(alphaColor)
        setStroke(borderWidth, secondaryColor)
    }

    val drawableRight: Drawable = mockDrawableRight ?: GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadii = floatArrayOf(
            0f, 0f, // top left
            radius, radius, // top right
            radius, radius, // bottom right
            0f, 0f //bottom left
        )
        color = ColorStateList.valueOf(alphaColor)
        setStroke(borderWidth, secondaryColor)
    }

    val drawableMiddle: Drawable = mockDrawableMiddle ?: GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        color = ColorStateList.valueOf(alphaColor)
        setStroke(borderWidth, secondaryColor)
    }

    private lateinit var render: SearchBgRender
    private val singleLineRender: SearchBgRender by lazy {
        SingleLineRender(
            padding, drawable
        )
    }
    private val multiLineRender: MultilineLineRender by lazy {
        MultilineLineRender(
            padding,
            drawableLeft,
            drawableMiddle,
            drawableRight
        )
    }
    private lateinit var spans: Array<out SearchSpan>
    private lateinit var headerSpans: Array<out HeaderSpan>

    private var spanStart = 0
    private var spanEnd = 0
    private var startLine = 0
    private var endLine = 0
    private var startOffset = 0
    private var endOffset = 0
    private var topExtraPadding = 0
    private var bottomExtraPadding = 0

    fun draw(canvas: Canvas, text: Spanned, layout: Layout) {
        spans = text.getSpans()
        Log.d("bg helper", "spans size = ${spans.size}")
        spans.forEach {
            spanStart = text.getSpanStart(it)
            spanEnd = text.getSpanEnd(it)
            startLine = layout.getLineForOffset(spanStart)
            endLine = layout.getLineForOffset(spanEnd)

            headerSpans = text.getSpans(spanStart, spanEnd, HeaderSpan::class.java)

            topExtraPadding = 0
            bottomExtraPadding = 0

            //if search
            if (it is SearchFocusSpan) {
                //if search focus invoke for focus
                focusListener(layout.getLineTop(startLine), layout.getLineBottom(startLine))
            }

            if (headerSpans.isNotEmpty()) {
                headerSpans[0].run {
                    this@SearchBgHelper.topExtraPadding =
                        if (spanStart in firstLineBounds || spanEnd in firstLineBounds) topExtraPadding else 0

                    this@SearchBgHelper.bottomExtraPadding =
                        if (spanStart in lastLineBounds || spanEnd in lastLineBounds) bottomExtraPadding else 0
                }
            }

            startOffset = layout.getPrimaryHorizontal(spanStart).toInt()  //начало и конец нашего поискового вхождения
            endOffset = layout.getPrimaryHorizontal(spanEnd).toInt()

            render = if (startLine == endLine) singleLineRender else multiLineRender
            render.draw(
                canvas,
                layout,
                startLine,
                endLine,
                startOffset,
                endOffset,
                topExtraPadding,
                bottomExtraPadding
            )
        }
    }


    abstract class SearchBgRender(
        val padding: Int
    ) {
        abstract fun draw(
            canvas: Canvas,
            layout: Layout,
            startLine: Int,
            endLine: Int,
            startOffset: Int,
            endOffset: Int,
            topExtraPadding: Int = 0,
            bottomExtraPadding: Int = 0,
        )

        fun getLineTop(layout: Layout, line: Int): Int {
            //return layout.getLineTop(line) // to do without padding
            return layout.getLineTopWithoutPadding(line)
        }

        fun getLineBottom(layout: Layout, line: Int): Int {
            //return layout.getLineBottom(line) // todo without padding
            return layout.getLineBottomWithoutPadding(line)
        }
    }

    class SingleLineRender(
        padding: Int,
        val drawable: Drawable
    ) : SearchBgRender(padding) {
        private var lineTop: Int = 0
        private var lineBottom: Int = 0

        override fun draw(
            canvas: Canvas,
            layout: Layout,
            startLine: Int,
            endLine: Int,
            startOffset: Int,
            endOffset: Int,
            topExtraPadding: Int,
            bottomExtraPadding: Int
        ) {
            lineTop = getLineTop(layout, startLine) + topExtraPadding
            lineBottom = getLineBottom(layout, endLine) - bottomExtraPadding
            drawable.setBounds(
                startOffset - padding,
                lineTop,
                endOffset + padding,
                lineBottom
            ) //не понял зачем -padding и +padding
            drawable.draw(canvas)
        }
    }

    class MultilineLineRender(
        padding: Int,
        private val drawableLeft: Drawable,
        private val drawableMiddle: Drawable,
        private val drawableRight: Drawable
    ) : SearchBgRender(padding) {
        private var lineTop: Int = 0
        private var lineBottom: Int = 0
        private var lineEndOffset: Int = 0
        private var lineStartOffset: Int = 0

        override fun draw(
            canvas: Canvas,
            layout: Layout,
            startLine: Int,
            endLine: Int,
            startOffset: Int,
            endOffset: Int,
            topExtraPadding: Int,
            bottomExtraPadding: Int
        ) {
            //draw first line
            lineEndOffset = (layout.getLineRight(startLine) + padding).toInt()
            lineTop = getLineTop(layout, startLine) + topExtraPadding
            lineBottom = getLineBottom(layout, startLine)
            drawStart(canvas, startOffset - padding, lineTop, lineEndOffset, lineBottom)
            //drawable.setBounds(startOffset, lineTop, endOffset, lineBottom)
            //drawable.draw(canvas)

            //draw middle line
            for (line in startLine.inc() until endLine) {
                lineTop = getLineTop(layout, line) + topExtraPadding
                lineBottom = getLineBottom(layout, line)
                drawableMiddle.setBounds(
                    layout.getLineLeft(line).toInt() - padding,
                    lineTop,
                    layout.getLineRight(line).toInt() + padding,
                    lineBottom
                )
                drawableMiddle.draw(canvas)
            }

            //draw last line
            lineStartOffset = (layout.getLineLeft(startLine) - padding).toInt()
            lineTop = getLineTop(layout, endLine)
            lineBottom = getLineBottom(layout, endLine) - bottomExtraPadding
            drawEnd(canvas, lineStartOffset, lineTop, endOffset + padding, lineBottom)
        }

        private fun drawStart(
            canvas: Canvas,
            start: Int,
            top: Int,
            end: Int,
            bottom: Int
        ) {
            drawableLeft.setBounds(start, top, end, bottom)
            drawableLeft.draw(canvas)
        }

        private fun drawEnd(
            canvas: Canvas,
            start: Int,
            top: Int,
            end: Int,
            bottom: Int
        ) {
            drawableRight.setBounds(start, top, end, bottom)
        }
    }
}