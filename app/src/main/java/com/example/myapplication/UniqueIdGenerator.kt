package com.example.myapplication

import java.util.PriorityQueue

object UniqueIdGenerator {
    private var counter = 10000
    private val freedIds = PriorityQueue<Int>()

    fun getUniqueId(): Int {
        return if (freedIds.isNotEmpty()) {
            freedIds.poll()!!
        } else {
            counter++
        }
    }

    fun releaseId(id: Int) {
        freedIds.offer(id)
    }
}