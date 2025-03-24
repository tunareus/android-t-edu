package com.example.dz_oop

interface Shop<out T : LibraryItem> {
    fun sell(): T
}