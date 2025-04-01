package com.example.myapplication

object UniqueIdGenerator {
    private var counter = 10000
    fun getUniqueId(): Int {
        return counter++
    }
}