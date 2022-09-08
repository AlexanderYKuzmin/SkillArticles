package ru.skillbranch.skillarticles.ui.custom.markdown

import android.text.Spannable
import android.text.SpannableString
import android.util.Log
import androidx.core.text.getSpans
import ru.skillbranch.skillarticles.ui.custom.spans.SearchFocusSpan
import ru.skillbranch.skillarticles.ui.custom.spans.SearchSpan

interface IMarkdownView {
    var fontSize: Float
    val spannableContent: Spannable

    fun renderSearchResult(
        results: List<Pair<Int, Int>>,
        offset: Int
    ) {
        clearSearchResult()
        //Log.e("IMarkdownView", "offset = $offset")
        //Log.e("IMarkdownView", "results = ${results.size}")
        val offsetResult = results
            .map { (start, end) -> start.minus(offset) to end.minus(offset)}
        Log.d("IMarkdownView", "offsetResult = ${offsetResult.size}")

        try {
            offsetResult.forEach {(start, end) ->
                Log.e("IMarkdownView", "start = $start, end = $end")
                spannableContent.setSpan(
                    SearchSpan(),
                    start,
                    end,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                Log.e("IMarkdownView", "spans : ${spannableContent.getSpans<SearchSpan>().size}")
            }
        } catch (e : Exception) {
            e.printStackTrace()
            Log.e("IMarkdownView", "${e.message}")
        }

    }

    fun renderSearchPosition(
        searchPosition: Pair<Int, Int>,
        offset: Int
    ) {
        clearSearchResult()
        spannableContent.setSpan(
            SearchFocusSpan(),
            searchPosition.first.minus(offset),
            searchPosition.second.minus(offset),
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    fun clearSearchResult() {
        spannableContent.getSpans<SearchSpan>().forEach {
            spannableContent.removeSpan(it)
        }
    }
}