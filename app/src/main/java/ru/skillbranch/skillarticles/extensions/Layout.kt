package ru.skillbranch.skillarticles.extensions

import android.text.Layout

// get the line height of a line
fun Layout.getLineHeight(line: Int): Int {
    return getLineTop(line + 1) - getLineTop(line)
}


//returns the top of the Layout after removing the extra padding applied by Layout
fun Layout.getLineTopWithoutPadding(line: Int): Int {
    var lineTop = getLineTop(line)
    if (line == 0) {
        lineTop -= topPadding
    }
    return lineTop
}

//returns the bottom of the layout after removing the extra padding applied by Layout
fun Layout.getLineBottomWithoutPadding(line: Int): Int {
    var lineBottom = getLineBottomWithoutSpacing(line)
    if (line == lineCount - 1) {
        lineBottom -= bottomPadding
    }
    return lineBottom
}

fun Layout.getLineBottomWithoutSpacing(line: Int): Int {
    val lineBottom = getLineBottom(line)
    val isLastLine = line == lineCount - 1
    val hasLineSpacing = spacingAdd != 0f

    val nextLineIsLast = line == lineCount - 2

    val onlyWhiteSpaceIsAfter = if (nextLineIsLast) {
        val start = getLineStart(line + 1)
        val lastVisible = getLineVisibleEnd(line + 1)
        start == lastVisible
    } else false

    return if (!hasLineSpacing || isLastLine || onlyWhiteSpaceIsAfter) {
        lineBottom + spacingAdd.toInt()
    } else {
        lineBottom - spacingAdd.toInt()
    }
}