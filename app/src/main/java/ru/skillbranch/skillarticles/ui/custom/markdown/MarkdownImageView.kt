package ru.skillbranch.skillarticles.ui.custom.markdown

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.text.Spannable
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.core.animation.doOnEnd
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.attrValue
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.extensions.dpToPx
import java.nio.charset.Charset
import java.security.MessageDigest
import kotlin.math.hypot

@SuppressLint("ViewConstructor")
class MarkdownImageView private constructor(
    context: Context,
    fontSize: Float
) : ViewGroup(context, null, 0), IMarkdownView {

    override var fontSize: Float = fontSize
    set(value) {
        tvTitle.textSize = value * 0.75f
        tvAlt?.textSize = value
        field = value
    }
    override val spannableContent: Spannable
    get() = tvTitle.text as Spannable

    //views
    lateinit var imageUrl: String
    lateinit var imageTitle: CharSequence

    private val ivImage: ImageView
    private val tvTitle: MarkdownTextView
    private var tvAlt: TextView? = null

    @Px
    private val titleTopMargin: Int = context.dpToIntPx(8)
    @Px
    private val titlePadding: Int = context.dpToIntPx(56)
    @Px
    private val cornerRadius: Float = context.dpToPx(4)
    @ColorInt
    private val colorSurface: Int = context.attrValue(R.attr.colorSurface)
    @ColorInt
    private val colorOnSurface: Int = context.attrValue(R.attr.colorOnSurface)
    @ColorInt
    private val colorOnBackground: Int = context.attrValue(R.attr.colorOnBackground)

    @ColorInt
    private var lineColor: Int = context.getColor(R.color.color_divider)

    //for draw object allocation
    private var linePositionY: Float = 0f
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        strokeWidth = 0f
    }

    init {
        ivImage = ImageView(context).apply {
            //setImageResource(R.mipmap.ic_launcher)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.ic_launcher_background)
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(
                        Rect(0, 0, view.measuredWidth, view.measuredHeight),
                        cornerRadius
                    )
                }
            }
            clipToOutline = true
        }
        addView(ivImage)

        tvTitle = MarkdownTextView(context, fontSize * 0.75f, false).apply {
            //setText("title", TextView.BufferType.SPANNABLE)
            setTextColor(colorOnBackground)
            gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            setPadding(titlePadding,0,titlePadding,0)
            //layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        addView(tvTitle)
    }

    constructor(
        context: Context,
        fontSize: Float,
        url: String,
        title: String,
        alt: String?
    ) : this(context, fontSize) {
        imageUrl = url
        imageTitle = title

        tvTitle.setText(title, TextView.BufferType.SPANNABLE)
        Log.d("MarkdownImage", "URL = $url")
        Glide
            .with(context)
            .load(url)
            .error(R.drawable.ic_launcher_background)
            .transform(AspectRatioResizeTransform())
            .into(ivImage)

        if (alt != null) {
            tvAlt = TextView(context).apply {
                text = alt
                setTextColor(colorOnSurface)
                setBackgroundColor(ColorUtils.setAlphaComponent(colorSurface, 160))
                gravity = Gravity.CENTER
                textSize = fontSize
                setPadding(titleTopMargin)
                isVisible = false
            }
            addView(tvAlt)
        }



        ivImage.setOnClickListener {
            if (tvAlt?.isVisible == true) animateHideAlt()
            else animateShowAlt()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var usedHeight = 0
        val width = View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)

        //create measurespec for children EXACTLY
        //all children width == parent width (constraint parent width)
        val ms = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)

        //Тогда это уже не нужно
        //measureChild(ivImage, widthMeasureSpec, heightMeasureSpec)
        //measureChild(tvTitle, widthMeasureSpec, heightMeasureSpec)

        //if (tvAlt != null) measureChild(tvAlt, widthMeasureSpec, heightMeasureSpec)
        //Пишем иначе
        ivImage.measure(ms, heightMeasureSpec)
        tvTitle.measure(ms, heightMeasureSpec)
        tvAlt?.measure(ms, heightMeasureSpec)


        usedHeight += ivImage.measuredHeight
        usedHeight += titleTopMargin
        linePositionY = usedHeight + tvTitle.measuredHeight / 2f
        usedHeight += tvTitle.measuredHeight

        setMeasuredDimension(width, usedHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var usedHeight = 0
        val bodyWidth = r - l - paddingLeft - paddingRight //right - left ...
        val left = paddingLeft
        val right = paddingLeft + bodyWidth

        ivImage.layout(
            left,
            usedHeight,
            right,
            usedHeight + ivImage.measuredHeight
        )

        usedHeight += ivImage.measuredHeight + titleTopMargin

        tvTitle.layout(
            left,
            usedHeight,
            right,
            usedHeight + tvTitle.measuredHeight
        )

        tvAlt?.layout(
            left,
            ivImage.measuredHeight - (tvAlt?.measuredHeight ?: 0),
            right,
            ivImage.measuredHeight
        )
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        canvas.drawLine(0f, linePositionY, titlePadding.toFloat(), linePositionY, linePaint)
        canvas.drawLine(canvas.width - titlePadding.toFloat(), linePositionY, canvas.width.toFloat(), linePositionY, linePaint)
    }

    private fun animateShowAlt() {
        tvAlt?.isVisible = true
        val endRadius = hypot(tvAlt?.width?.toFloat() ?: 0f, tvAlt?.height?.toFloat() ?: 0f)
        val va = ViewAnimationUtils.createCircularReveal(
            tvAlt,
            tvAlt?.width ?: 0,
            tvAlt?.height ?: 0,
            0f,
            endRadius
        )
        va.start()
    }

    private fun animateHideAlt() {
        tvAlt?.isVisible = true
        val endRadius = hypot(tvAlt?.width?.toFloat() ?: 0f, tvAlt?.height?.toFloat() ?: 0f)
        val va = ViewAnimationUtils.createCircularReveal(
            tvAlt,
            tvAlt?.width ?: 0,
            tvAlt?.height ?: 0,
            endRadius,
            0f
        )
        va.doOnEnd { tvAlt?.isVisible = false }
        va.start()
    }
}

class AspectRatioResizeTransform : BitmapTransformation() {
    private val ID = "ru.skillbranch.skillarticles.glide.AspectRatioResizeTransform" // any unique string
    private val ID_BYTES = ID.toByteArray(Charset.forName("UTF-8"))

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val originWidth = toTransform.width
        val originHeight = toTransform.height
        val aspectRatio = originWidth.toFloat() / originHeight
        return Bitmap.createScaledBitmap(
            toTransform,
            outWidth,
            (outWidth / aspectRatio).toInt(),
            true
        )
    }

    override fun equals(other: Any?): Boolean = other is AspectRatioResizeTransform

    override fun hashCode(): Int = ID.hashCode()
}