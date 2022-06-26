package ru.skillbranch.skillarticles.extensions

import android.util.Log

fun String?.indexesOf(query: String, ignoreCase: Boolean = true): List<Int> {
    if (this == null || query.isEmpty()) return emptyList()
    var index = 0
    val indexes = mutableListOf<Int>()

    while (true) {
        index = indexOf(query, index, ignoreCase)
        index += if (index != -1) {
            Log.d("String.indexes", "index = $index, list.size = ${indexes.size}")
            indexes.add(index)
            query.length
        } else {
            Log.d("String.indexes", "results count: ${indexes.size}")
            return indexes
        }
    }
}