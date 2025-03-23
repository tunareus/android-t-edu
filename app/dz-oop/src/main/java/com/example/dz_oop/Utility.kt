package com.example.dz_oop

inline fun <reified T> List<Any>.filterByType(): List<T> {
    return this.filterIsInstance<T>()
}