package com.example.myapplication

import java.util.LinkedList
import java.util.Queue

object UniqueIdGenerator {
    private var counter: Int = 10000
    private val freedIds: Queue<Int> = LinkedList()

    fun getUniqueId(): Int {
        return if (freedIds.isNotEmpty()) {
            freedIds.poll()!!
        } else {
            counter++
        }
    }

    fun releaseId(id: Int) {
        freedIds.add(id)
    }

    fun reset() {
        counter = 10000
        freedIds.clear()
    }
}