package com.example.myapplication.common.util

inline fun <reified T> List<*>.filterByType(): List<T> {
    val result = mutableListOf<T>()
    for (item in this) {
        if (item is T) {
            result.add(item)
        }
    }
    return result
}